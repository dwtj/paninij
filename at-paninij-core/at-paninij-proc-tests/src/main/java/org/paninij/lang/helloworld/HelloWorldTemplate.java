package org.paninij.lang.helloworld;

import org.paninij.lang.Capsule;
import org.paninij.lang.Local;

@Capsule
class HelloWorldTemplate
{
    @Local Console c;
    @Local Greeter g;

    void design(HelloWorld self) {
        g.imports(c);
    }

    void run() {
        int ret2 = g.greetBlock();
        System.out.println("Greet has blocked and returned: " + ret2);
    }
}
