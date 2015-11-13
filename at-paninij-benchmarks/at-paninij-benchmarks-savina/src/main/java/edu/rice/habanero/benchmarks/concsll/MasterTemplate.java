package edu.rice.habanero.benchmarks.concsll;

import org.paninij.lang.Capsule;
import org.paninij.lang.Local;

@Capsule public class MasterTemplate {

    @Local Worker[] workers = new Worker[SortedListConfig.NUM_ENTITIES];
    @Local SortedList sortedList;

    int numWorkersTerminated = 0;

    public void design(Master self) {
        for (int i = 0; i < workers.length; i++) workers[i].imports(self, sortedList, i);
        sortedList.imports(workers);
    }

    public void start() {
        for (Worker w : workers) w.doWork();
    }

    public void workerFinished() {
        numWorkersTerminated++;
        if (numWorkersTerminated == SortedListConfig.NUM_ENTITIES) {
            sortedList.printResult();
            for (Worker w : workers) w.exit();
        }
    }
}