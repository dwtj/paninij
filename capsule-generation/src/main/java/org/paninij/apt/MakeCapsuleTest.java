package org.paninij.apt;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.TypeElement;

import org.paninij.apt.util.PaniniModelInfo;
import org.paninij.apt.util.Source;
import org.paninij.model.Capsule;

public class MakeCapsuleTest extends MakeCapsule$Thread
{
    static MakeCapsuleTest make(PaniniProcessor context, TypeElement template, Capsule capsule)
    {
        MakeCapsuleTest cap = new MakeCapsuleTest();
        cap.context = context;
        cap.template = template;
        cap.capsule = capsule;
        return cap;
    }


    @Override
    List<String> buildMain()
    {
        // TODO: Fix this hack.
        return buildTests();
    }
    

    List<String> buildTests()
    {
        List<String> src = Source.lines();
        int num_tests = buildProcedureIDs().size();
        for (int idx = 0; idx < num_tests; idx++)
        {
            src.addAll(buildTest(idx));
            src.add("");
        }
        return src;
    }
    

    List<String> buildTest(int test_id)
    {
        // Notice that the instance of the class test in which the tests are run is never used to
        // perform an actual test. A new test instance (`capsule_test`) is generated by each test.
        List<String> src = Source.lines("@Test",
                                        "public void panini$test#0()",
                                        "{",
                                        "    Panini$Message test_msg = new SimpleMessage(#0);",
                                        "    Panini$Message exit_msg = new SimpleMessage(PANINI$SHUTDOWN);",
                                        "    #1 capsule_test = new #1();",
                                        "    capsule_test.panini$push(test_msg);",
                                        "    capsule_test.panini$push(exit_msg);",
                                        "    capsule_test.run();",
                                        "}");
        return Source.formatAll(src, test_id, buildCapsuleName());
    }
    
    @Override
    String buildCapsuleName() {
        return PaniniModelInfo.simpleTesterName(template);
    }
    
    @Override
    String buildQualifiedCapsuleName() {
        return PaniniModelInfo.qualifiedTesterName(template);
    }

    @Override
    String buildCapsuleDecl() {
        return Source.format("public class #0 extends Capsule$Thread", buildCapsuleName());
    }

    @Override
    Set<String> getStandardImports()
    {
        Set<String> imports = super.getStandardImports();
        imports.add("org.paninij.runtime.SimpleMessage");
        imports.add("org.junit.Test");
        return imports;
    }
}
