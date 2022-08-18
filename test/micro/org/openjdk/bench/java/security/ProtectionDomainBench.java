package org.openjdk.bench.java.security;

import java.security.*;
import java.net.*;
import java.io.*;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import org.openjdk.bench.util.InMemoryJavaCompiler;

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
public class ProtectionDomainBench {

    @Param({"10", "100"})
    public int numberOfClasses = 50;

    final String host = "localhost";

    URL u, u2;

    @Setup
    public void setup() throws IOException {
        u = new URL("file:/tmp/duke");
        u2 = new URL("file:/tmp/foo");

        p = new Permissions();
        p.add(new SocketPermission(host, "connect"));

    }

    static byte[][] compiledClasses;
    static Class[] loadedClasses;
    static int index = 0;
    CodeSource cs, cs2;
    Permissions p, p2;

    static String B(int count) {
        return new String("public class B" + count + " {"
                + "   static int intField;"
                + "   public static void compiledMethod() { "
                + "       intField++;"
                + "   }"
                + "}");
    }

    @Setup
    public void setupClasses() throws Exception {
        compiledClasses = new byte[numberOfClasses][];
        loadedClasses = new Class[numberOfClasses];
        for (int i = 0; i < numberOfClasses; i++) {
            compiledClasses[i] = InMemoryJavaCompiler.compile("B" + i, B(i));
        }

        cs = new CodeSource(u, (java.security.cert.Certificate[]) null);
        cs2 =new CodeSource(u2, (java.security.cert.Certificate[]) null);
        p = new Permissions();
        p.add(new SocketPermission(host, "connect"));
        p.add(new SocketPermission(host, "accept"));
        p.add(new SocketPermission(host, "listen"));
        p.add(new SocketPermission(host, "resolve"));

        p2 = new Permissions();
        p2.add(new FilePermission("/tmp", "read"));
        p2.add(new FilePermission("/tmp", "write"));
        p2.add(new FilePermission("/tmp", "execute"));
        p2.add(new FilePermission("/tmp", "delete"));
    }

    static class ProtectionDomainBenchLoader extends ClassLoader {

        ProtectionDomainBenchLoader() {
            super();
        }

        ProtectionDomainBenchLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals("B" + index)) {
                assert compiledClasses[index]  != null;
                return defineClass(name, compiledClasses[index] , 0, (compiledClasses[index]).length);
            } else {
                return super.findClass(name);
            }
        }
    }

    @Benchmark
    @Fork(value = 3, jvmArgsAppend={"-Djava.security.manager=allow"})
    public void bench()  throws ClassNotFoundException {

        ProtectionDomainBench.ProtectionDomainBenchLoader loader1 = new
                ProtectionDomainBench.ProtectionDomainBenchLoader();
        ProtectionDomain pd1 = new ProtectionDomain(cs, p, loader1, null);
        ProtectionDomain pd2 = new ProtectionDomain(cs2, p2, loader1, null);

        for (index = 0; index < compiledClasses.length; index++) {
            String name = new String("B" + index);
            Class c = loader1.findClass(name);
            loadedClasses[index] = c;
        }
    }
}
