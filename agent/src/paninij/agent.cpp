#include <jvmti.h>
#include <cassert>
#include "paninij/agent.h"
#include "paninij/ownership.h"

/*****************************************************************************
 * Global Symbolic Constants                                                 *
 *****************************************************************************/

namespace {
    /**
     * Indicates that a heap traversal should not limit callbacks based on
     * whether an object or its class is tagged.
     *
     * @see
     *    <a href="https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#jvmtiHeapFilter">Heap Filter Flags</a>
     */
    const jint DONT_FILTER_HEAP_CALLBACKS_BY_TAG = 0;

    /**
     * Indicates that a heap traversal should not limit callbacks based on
     * an object's class.
     *
     * @see
     *     <a href="https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#FollowReferences.klass"
     */
    const jclass DONT_FILTER_HEAP_CALLBACKS_BY_CLASS = nullptr;

    /** Indicates that an object has no tag. */
    const jlong NO_TAG = 0;

    /** A heap tag which indicates that an object is being moved. */
    const jlong MOVE_TAG = 1;

    /**
     * A heap tag which indicates that an owned object was found to be illegally
     * moved: this moved object it is still owned-by (i.e. reachable-from) the
     * sender.
     */
    const jlong ILLEGAL_MOVE_TAG = 2;
}


/*****************************************************************************
 * Agent Global State                                                        *
 *****************************************************************************/

jvmtiEnv* jvmti_env;       // TODO: Is there anything unsafe about storing this?


/*****************************************************************************
 * Agent Initialization                                                      *
 *****************************************************************************/

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM* jvm, char* options, void* reserved) {
    jvmtiEnv* env;

    if (JNI_EVERSION == jvm->GetEnv((void **) &env, JVMTI_VERSION_1_2)) {
        env = nullptr;
        return JNI_ERR;
    }
    if (! add_capabilities(env) ||
        ! enable_events(env) ||
        ! set_event_callbacks(env))
    {
        jvmtiError err = env->DisposeEnvironment();
        env = nullptr;
        return JNI_ERR;
    }
    jvmti_env = env;
    return JNI_OK;
}


bool add_capabilities(jvmtiEnv* env) {
    jvmtiCapabilities potential;
    jvmtiError err = env->GetPotentialCapabilities(&potential);
    if (err != JVMTI_ERROR_NONE) return false;
    if (! potential.can_tag_objects) return false;
    if (! potential.can_generate_method_entry_events) return false;

    env->AddCapabilities(&agent_capabilities);

    return true;
}


bool enable_events(jvmtiEnv* env) {
    jthread t = nullptr;
    for (jvmtiEvent& ev : enabled_events) {
        jvmtiError err = env->SetEventNotificationMode(JVMTI_ENABLE, ev, t);
        if (err != JVMTI_ERROR_NONE) {
            return false;
        }
    }
    return true;
}


bool set_event_callbacks(jvmtiEnv* env) {
    jvmtiError err = env->SetEventCallbacks(&agent_callbacks,
                                            sizeof(jvmtiEventCallbacks));
    return err == JVMTI_ERROR_NONE;
}


/*****************************************************************************
 * Agent Callbacks                                                           *
 *****************************************************************************/

static void JNICALL
vm_object_alloc_cb(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                   jobject object, jclass object_klass, jlong size) {
    std::cout << "vm_object_alloc_cb()" << std::endl;
}


static jint JNICALL
heap_tagging_cb(jvmtiHeapReferenceKind reference_kind,
                  const jvmtiHeapReferenceInfo* reference_info,
                  jlong, // class_tag
                  jlong, // referrer_class_tag
                  jlong, // size
                  jlong* tag_ptr,
                  jlong* referrer_tag_ptr,
                  jint,  // length
                  void*) // user_data
{
    if (referrer_tag_ptr == nullptr) {
        // The current object is the root of the object graph being explored.
        // Nothing to do.
    } else {
        *tag_ptr = *referrer_tag_ptr;
    }
    return JVMTI_VISIT_OBJECTS;
}


static jint JNICALL
heap_searching_cb(jvmtiHeapReferenceKind reference_kind,
                  const jvmtiHeapReferenceInfo* reference_info,
                  jlong,  // class_tag
                  jlong,  // referrer_class_tag
                  jlong,  // size
                  jlong*  tag_ptr,
                  jlong*, //referrer_tag_ptr
                  jint,   // length
                  void* found_illegal_move)
{
    if (*tag_ptr == MOVE_TAG) {
        *tag_ptr = ILLEGAL_MOVE_TAG;
        *(bool*) found_illegal_move = true;
    }
    return JVMTI_VISIT_OBJECTS;
}


/*****************************************************************************
 * Agent Shutdown                                                            *
 *****************************************************************************/

JNIEXPORT void JNICALL
Agent_OnUnload(JavaVM *vm) {
    // Nothing to do yet.
}


/*****************************************************************************
 * Definitions of JNI Methods                                                *
 *****************************************************************************/

void tagAllReachable(jobject root, jlong tag) {
    jvmtiError err;
    err = jvmti_env->SetTag(root, tag);
    assert(err == JVMTI_ERROR_NONE);
    err = jvmti_env->FollowReferences(DONT_FILTER_HEAP_CALLBACKS_BY_TAG,
                                      DONT_FILTER_HEAP_CALLBACKS_BY_CLASS,
                                      root, &heap_tagging_callbacks, nullptr);
    assert(err == JVMTI_ERROR_NONE);
}


/**
 * This is called by the PaniniJ runtime to report that the client's program has
 * moved ownership of the object graph rooted at `ref` from the `sender` capsule
 * to the `reciever` capsule. The given `clazz` is always the `Ownership` class
 * itself.
 */
JNIEXPORT void JNICALL
Java_org_paninij_runtime_check_Ownership_move(JNIEnv*  jni_env,
                                              jclass,  // Ownership.class
                                              jobject  sender,
                                              jobject, // receiver (cur. unused)
                                              jobject  ref)
{
    if (sender == nullptr || ref == nullptr) {
        return;
    }

    // Notice that under the assumption of strong ownership and state
    // encapsulation, there are no data-races between these different calls to
    // `FollowReferences()`. This is because under these assumptions, the only
    // Java thread which may modify the `sender` or `ref` object graphs is the
    // Java thread which called this JNI method.

    jvmtiError err;
    bool found_illegal_move;
    tagAllReachable(ref, MOVE_TAG);

    // Search for objects reachable from `sender` but marked with `MOVE_TAG`.
    err = jvmti_env->FollowReferences(DONT_FILTER_HEAP_CALLBACKS_BY_TAG,
                                      DONT_FILTER_HEAP_CALLBACKS_BY_CLASS,
                                      sender,
                                      &heap_searching_callbacks,
                                      &found_illegal_move);
    assert(err == JVMTI_ERROR_NONE);

    if (found_illegal_move) {
        // TODO: Consider using `GetObjectsWithTags()`
        jclass error_class = jni_env->FindClass("java/lang/Error");
        jni_env->ThrowNew(error_class, "Detected an illegal ownership move.");
    } else {
        // Otherwise, un-tag all objects reachable from `ref`
        tagAllReachable(ref, NO_TAG);
    }
}
