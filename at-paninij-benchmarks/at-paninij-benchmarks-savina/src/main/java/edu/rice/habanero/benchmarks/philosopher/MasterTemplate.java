package edu.rice.habanero.benchmarks.philosopher;

import org.paninij.lang.Capsule;
import org.paninij.lang.Local;
import org.paninij.lang.Root;

@Root
@Capsule
public class MasterTemplate {
    @Local Arbiter arbiter;

    public void run() {
        arbiter.start();
    }
}