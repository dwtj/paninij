package edu.rice.habanero.benchmarks.threadring;

import org.paninij.lang.Capsule;
import org.paninij.lang.Child;

@Capsule public class ThreadRingTemplate {
    @Child Node[] nodes = new Node[ThreadRingConfig.N];

    public void design(ThreadRing self) {
        for (int i = 0; i < ThreadRingConfig.N; i++)
            nodes[i].wire(nodes[(i + 1) % ThreadRingConfig.N]);
    }

    public void run() {
        nodes[0].ping(ThreadRingConfig.R);
    }

}