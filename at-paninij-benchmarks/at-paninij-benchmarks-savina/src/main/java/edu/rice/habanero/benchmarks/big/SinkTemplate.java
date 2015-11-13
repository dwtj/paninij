package edu.rice.habanero.benchmarks.big;

import org.paninij.lang.Capsule;
import org.paninij.lang.Imports;

@Capsule public class SinkTemplate {
    @Imports Node[] nodes = new Node[BigConfig.W];
    int numMessages = 0;

    public void start() {
        for (Node n : nodes) n.pong(-1);
    }

    public void finished() {
        numMessages++;
        if (numMessages == BigConfig.W) {
            for (Node n : nodes) n.done();
        }
    }

}