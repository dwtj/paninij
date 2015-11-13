package edu.rice.habanero.benchmarks.radixsort;

import org.paninij.lang.Capsule;
import org.paninij.lang.Local;
import org.paninij.lang.Root;

@Root
@Capsule
public class RadixSortTemplate
{
    int sortCount = (int) (Math.log(RadixSortConfig.M) / Math.log(2));

    @Local Validation validator;
    @Local IntSource source;
    @Local Sort[] sorters = new Sort[sortCount];

    public void design(RadixSort self) {
        long radix = RadixSortConfig.M /2;
        Adder next = validator;

        for (int i = 0; i < sortCount; i++) {
            sorters[i].imports(next, radix);
            next = sorters[i];
            radix /= 2;
        }

        source.imports(next);
    }

    public void run() {
        source.start();
    }
}