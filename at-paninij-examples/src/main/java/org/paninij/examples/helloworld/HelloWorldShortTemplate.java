package org.paninij.examples.helloworld;

import org.paninij.lang.Capsule;
import org.paninij.lang.CapsuleSystem;


@Capsule
public class HelloWorldShortTemplate
{
    void run() {
        System.out.println("@PaniniJ: Hello World!");
    }
    
    public static void main(String[] args) {
        CapsuleSystem.start("org.paninij.examples.helloworld.HelloWorldShort", args);
    }
}
