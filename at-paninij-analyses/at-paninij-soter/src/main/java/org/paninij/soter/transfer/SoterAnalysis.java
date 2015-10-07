package org.paninij.soter.transfer;

import static org.paninij.soter.util.SoterUtil.makePointsToClosure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.paninij.runtime.util.IdentitySet;
import org.paninij.runtime.util.IntMap;
import org.paninij.soter.cga.CallGraphAnalysis;
import org.paninij.soter.live.CallGraphLiveAnalysis;
import org.paninij.soter.live.TransferLiveAnalysis;
import org.paninij.soter.model.CapsuleTemplate;
import org.paninij.soter.site.TransferringSite;
import org.paninij.soter.site.SiteAnalysis;
import org.paninij.soter.site.SiteFactory;
import org.paninij.soter.util.AnalysisJsonResultsCreator;
import org.paninij.soter.util.LoggingAnalysis;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSetFactory;

public class SoterAnalysis extends LoggingAnalysis
{
    // The analysis's dependencies:
    protected final CapsuleTemplate template;
    protected final CallGraphAnalysis cga;
    protected final SiteAnalysis sa;
    protected final TransferLiveAnalysis tla;
    protected final CallGraphLiveAnalysis cgla;
    protected final IClassHierarchy cha;


    protected final MutableSparseIntSetFactory intSetFactory;

    /**
     * This analysis adds its results to this map as they are generated. The keys are the
     * transferring `TransferSite` objects generated by the `TransferAnalysis` and values are
     * generated `TransferSiteResults` object.
     */
    protected final Map<TransferringSite, TransferSiteResults> transferSiteResultsMap;


    protected final Map<IMethod, Set<TransferringSite>> unsafeTransferringSitesMap;


    protected final JsonResultsCreator jsonCreator;


    public SoterAnalysis(CapsuleTemplate template, CallGraphAnalysis cga, SiteAnalysis ta,
                         TransferLiveAnalysis tla, CallGraphLiveAnalysis cgla, IClassHierarchy cha)
    {
        this.template = template;
        this.cga = cga;
        this.sa = ta;
        this.tla = tla;
        this.cgla = cgla;
        this.cha = cha;

        intSetFactory = new MutableSparseIntSetFactory();

        transferSiteResultsMap = new HashMap<TransferringSite, TransferSiteResults>();
        unsafeTransferringSitesMap = new HashMap<IMethod, Set<TransferringSite>>();

        jsonCreator = new JsonResultsCreator();
    }


    @Override
    protected void performSubAnalyses()
    {
        cga.perform();
        sa.perform();
        tla.perform();
        cgla.perform();
    }


    @Override
    protected void performAnalysis()
    {
        buildTransferSiteResultsMap();
        buildUnsafeTransfersMap();
    }


    protected void buildTransferSiteResultsMap()
    {
        for (CGNode transferringNode : sa.getTransferringNodes())
        {
            for (TransferringSite transferSite : sa.getTransferringSites(transferringNode))
            {
                TransferSiteResults results = new TransferSiteResults();

                // Find all of the live variables after this transfer site.
                results.liveVariables = new HashSet<PointerKey>();
                results.liveVariables.addAll(tla.getLiveVariablesAfter(transferSite));
                results.liveVariables.addAll(cgla.getLiveVariablesAfter(transferringNode));

                // Find all of the (transitively) live objects.
                results.liveObjects = new HashSet<InstanceKey>();
                for (PointerKey rootLivePtr : results.liveVariables)
                {
                    for (InstanceKey liveObj: makePointsToClosure(rootLivePtr, cga.getHeapGraph()))
                    {
                        results.liveObjects.add(liveObj);
                    }
                }

                // For each of the transfer site's transfers, find all of the (transitively)
                // escaped objects.
                HeapModel heapModel = cga.getHeapModel();
                HeapGraph<InstanceKey> heapGraph = cga.getHeapGraph();
                IntIterator paramIter = transferSite.getTransfers().intIterator();
                while (paramIter.hasNext())
                {
                    int paramID = paramIter.next();

                    PointerKey ptr = heapModel.getPointerKeyForLocal(transferringNode, paramID);
                    Set<InstanceKey> escaped = makePointsToClosure(ptr, heapGraph).cloneAsSet();
                    results.setEscapedObjects(paramID, escaped);

                    boolean isSafeTransfer = isDisjointFrom(escaped, results.liveObjects);
                    results.setTransferSafety(paramID, isSafeTransfer);
                }

                transferSiteResultsMap.put(transferSite, results);
            }
        }
    }
    
    
    private static boolean isDisjointFrom(Set<InstanceKey> escapedObjs, Set<InstanceKey> liveObjs)
    {
        for (InstanceKey escaped: escapedObjs) {
            if (liveObjs.contains(escaped)) {
                return false;
            }
        }
        return true;
    }


    protected void buildUnsafeTransfersMap()
    {
        for (Entry<TransferringSite, TransferSiteResults> entry : transferSiteResultsMap.entrySet())
        {
            TransferringSite transferSite = entry.getKey();
            TransferSiteResults results = entry.getValue();

            if (results.hasUnsafeTransfers())
            {
                IMethod method = transferSite.getNode().getMethod();
                Set<TransferringSite> unsafeTransferSites = getOrMakeUnsafeTransferringSites(method);
                TransferringSite unsafeTransferSite = SiteFactory.copyWith(transferSite, results.getUnsafeTransfers());
                unsafeTransferSites.add(unsafeTransferSite);
            }
        }
    }

    /**
     * Gets and returns the set of unsafe transfer sites associated with the given method. If there
     * isn't yet such a set in the map, then an empty set is created, added to the map, and
     * returned.
     */
    private Set<TransferringSite> getOrMakeUnsafeTransferringSites(IMethod method)
    {
        Set<TransferringSite> unsafeTransferringSites = unsafeTransferringSitesMap.get(method);
        if (unsafeTransferringSites == null)
        {
            unsafeTransferringSites = new HashSet<TransferringSite>();
            unsafeTransferringSitesMap.put(method, unsafeTransferringSites);
        }
        return unsafeTransferringSites;
    }


    public CallGraph getCallGraph()
    {
        return cga.getCallGraph();
    }


    public HeapGraph<InstanceKey> getHeapGraph()
    {
        return cga.getHeapGraph();
    } 


    public CapsuleTemplate getCapsuleTemplate()
    {
        return template;
    }


    /**
     * @return The map of primary results generated by this analysis. The map's keys are methods on
     *         the capsule template which have been found to have unsafe transfer sites. The value
     *         associated with a method is the set of transfer sites within this method which have
     *         been found by the analysis to have unsafe transfers. The `transfers` on these
     *         `TransferSite` instances are the set of transfers which were found to be *unsafe*
     *         (rather than all of the transfers at that transfer site).
     * 
     * TODO: Consider removing this from the interface.
     */
    public Map<IMethod, Set<TransferringSite>> getUnsafeTransferSitesMap()
    {
        return unsafeTransferringSitesMap;
    }
    
    
    public SiteAnalysis getTransferAnalysis()
    {
        assert hasBeenPerformed;
        return sa;
    }


    @Override
    public JsonObject getJsonResults()
    {
        assert hasBeenPerformed;
        return jsonCreator.toJson();
    }


    @Override
    public String getJsonResultsString()
    {
        assert hasBeenPerformed;
        return jsonCreator.toJsonString();
    }


    @Override
    public String getJsonResultsLogFileName()
    {
        return template.getQualifiedName().replace('/', '.') + ".json";
    }
    
    
    /**
     * A simple container class to hold all of the results which the analysis generates for a single
     * transfer site.
     */
    final class TransferSiteResults
    {
        Set<PointerKey> liveVariables;
        Set<InstanceKey> liveObjects;
        MutableIntSet unsafeTransfers;
        MutableIntSet safeTransfers;
        IntMap<Set<InstanceKey>> escapedObjectsMap;

        public TransferSiteResults()
        {
            unsafeTransfers = intSetFactory.make();
            safeTransfers = intSetFactory.make();
            escapedObjectsMap = new IntMap<Set<InstanceKey>>();
        }

        public void setEscapedObjects(int transferID, Set<InstanceKey> escapedObjects)
        {
            escapedObjectsMap.put(transferID, escapedObjects);
        }

        public Set<InstanceKey> getEscapedObjects(int transferID)
        {
            return escapedObjectsMap.get(transferID);
        }

        public void setTransferSafety(int transferID, boolean isSafeTransfer)
        {
            if (isSafeTransfer) {
                safeTransfers.add(transferID);
                unsafeTransfers.remove(transferID);
            } else {
                safeTransfers.remove(transferID);
                unsafeTransfers.add(transferID);
            }
        }

        public boolean isSafeTransfer(int transferID)
        {
            return safeTransfers.contains(transferID);
        }

        public boolean isUnsafeTransfer(int transferID)
        {
            return unsafeTransfers.contains(transferID);
        }

        public IntSet getUnsafeTransfers()
        {
            return unsafeTransfers;
        }

        public IntSet getSafeTransfers()
        {
            return safeTransfers;
        }

        public boolean hasUnsafeTransfers()
        {
            return !unsafeTransfers.isEmpty();
        }
    }
    
    private class JsonResultsCreator extends AnalysisJsonResultsCreator
    {
        public JsonObject toJson()
        {
            assert hasBeenPerformed;

            if (json != null) {
                return json;
            }

            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("capsuleTemplate", template.getQualifiedName());

            /*
            //JsonObjectBuilder tempObjectBuilder;
            JsonArrayBuilder methodsArrayBuilder = Json.createArrayBuilder();
            builder.add("methods", methodsArrayBuilder);

            // Make an array of all transfer sites.
            for (Entry<IMethod, IdentitySet<TransferSite>> entry : analysis.unsafeTransferSitesMap.entrySet())
            {
                IMethod method = entry.getKey();
                methodsArrayBuilder.add(method.getSignature());
                JsonObjectBuilder methodSummary

                // For each method, make an array of transfer sites.
                IdentitySet<TransferSite> unsafeTransferSites = entry.getValue();
                for (TransferSite unsafeTransferSite : unsafeTransferSites)
                {
                    reportWriter.append("    ");
                    jsonWriter.write(unsafeTransferSite.toJson());
                }
            }
            */

            // Make an array of all transfer sites.
            JsonArrayBuilder transferSitesBuilder = Json.createArrayBuilder();
            for (Entry<TransferringSite, TransferSiteResults> entry : transferSiteResultsMap.entrySet())
            {
                transferSitesBuilder.add(toJson(entry.getKey(), entry.getValue()));
            }
            builder.add("transferSites", transferSitesBuilder);

            json = builder.build();
            return json;
        }

        private JsonObject toJson(TransferringSite site, TransferSiteResults results)
        {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("transferringSite", site.toJson())
                   .add("liveVariables", toJson(results.liveVariables))
                   .add("liveObjects", instanceKeysToJson(results.liveObjects));

            // Create an array of JSON objects describing the analysis results for each of the
            // current transfer site's transfers.
            IntIterator transfersIter = site.getTransfers().intIterator();
            JsonArrayBuilder transfersArrayBuilder = Json.createArrayBuilder();
            while (transfersIter.hasNext())
            {
                int transfer = transfersIter.next();
                JsonObjectBuilder transferBuilder = Json.createObjectBuilder();
                transferBuilder.add("transferID", transfer)
                               .add("escapedObjects", instanceKeysToJson(results.getEscapedObjects(transfer)))
                               .add("isSafeTransfer", results.isSafeTransfer(transfer));
                transfersArrayBuilder.add(transferBuilder);
            }
            builder.add("transfers", transfersArrayBuilder);
            return builder.build();
        }
        
        public CallGraph getCallGraph()
        {
            return cga.getCallGraph();
        }
    }
}
