/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.vm.runtime;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import org.openjdk.bench.util.InMemoryJavaCompiler;

@State(Scope.Benchmark)
@Warmup(iterations = 18, time = 5)
@Measurement(iterations = 10, time = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
public class MethodHandleStress {

    // The number of distinct classes generated from the source string below
    // All the classes are "warmed up" by invoking their methods to get compiled by the jit
    @Param({"1000"})
    public int classes;

    // How many instances of each generated class to create and call in the measurement phase
    @Param({"100"})
    public int instances;

    byte[][] compiledClasses;
    Class[] loadedClasses;
    String[] classNames;

    int index = 0;
    Map<Object, Method[]> classToMethodsMap = new HashMap<>();
    Map<Class, Object[]> instancesOfClassMap = new HashMap<>();

    Map<Class, Map<Object, MethodHandle>> prebindMethods = new ConcurrentHashMap<>();

    static final String methodNames[] = {
            "get"
    };
    static String newLine = System.getProperty("line.separator");

    static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    static String nextText(int size) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();

        String word = tlr.ints(0, 52).limit(size).boxed().
                map(x -> alphabet.charAt(x)).
                map(x -> x.toString()).
                collect(Collectors.joining());

        return word;
    }

    static String B(int count, String filler) {
        return "import java.util.*; "
                + "import java.nio.file.*;"
                + "import java.lang.invoke.*;"
                + "import java.util.function.*;"
                + " "
                + "public class B" + count + " {"
                + " "
                + "    public String toString() {"
                + newLine
                + "        return this.getClass().getName() + \", targetMethod=\" + targetMethod;"
                + newLine
                + "    }"
                + " "
                + newLine
                + "   static int myId" + " = " + count + ";"
                + newLine
                + " "
                + "    public synchronized int getMyId() {"
                + "       return myId;"
                + "    }"
                + " "
                + "    static int intFieldA" + filler + " = 0;"
                + "    static int staticPadAA" + filler + " = 0;"
                + "    static int staticPadAB" + filler + " = 0;"
                + "    static int staticPadAC" + filler + " = 0;"
                + "    static int staticPadAD" + filler + " = 0;"
                + "    static int staticPadAE" + filler + " = 0;"
                + "    static int staticPadAF" + filler + " = 0;"
                + "    static int staticPadAG" + filler + " = 0;"
                + "    static int staticPadAH" + filler + " = 0;"
                + "    static int staticPadAI" + filler + " = 0;"
                + "    static int staticPadAJ" + filler + " = 0;"
                + "    static int staticPadAK" + filler + " = 0;"
                + "    static int staticPadAL" + filler + " = 0;"
                + "    static int staticPadAM" + filler + " = 0;"
                + "    static int staticPadAN" + filler + " = 0;"
                + "    static int staticPadAO" + filler + " = 0;"
                + "    static int staticPadAP" + filler + " = 0;"
                + "    static int staticPadAQ" + filler + " = 0;"
                + "    static int staticPadAR" + filler + " = 0;"
                + "    static int staticPadAS" + filler + " = 0;"
                + "    static int staticPadAT" + filler + " = 0;"
                + " "
                + "    static int getStaticA() {"
                + "        return intFieldA" + filler + ";"
                + "    }"
                + " "
                + "    static void setStaticA(int x) {"
                + "        intFieldA" + filler + " = x;"
                + "    }"
                + " "
                + "    static int intFieldB" + filler + " = 0;"
                + "    static int staticPadBA" + filler + " = 0;"
                + "    static int staticPadBB" + filler + " = 0;"
                + "    static int staticPadBC" + filler + " = 0;"
                + "    static int staticPadBD" + filler + " = 0;"
                + "    static int staticPadBE" + filler + " = 0;"
                + "    static int staticPadBF" + filler + " = 0;"
                + "    static int staticPadBG" + filler + " = 0;"
                + "    static int staticPadBH" + filler + " = 0;"
                + "    static int staticPadBI" + filler + " = 0;"
                + "    static int staticPadBJ" + filler + " = 0;"
                + "    static int staticPadBK" + filler + " = 0;"
                + "    static int staticPadBL" + filler + " = 0;"
                + "    static int staticPadBM" + filler + " = 0;"
                + "    static int staticPadBN" + filler + " = 0;"
                + "    static int staticPadBO" + filler + " = 0;"
                + "    static int staticPadBP" + filler + " = 0;"
                + "    static int staticPadBQ" + filler + " = 0;"
                + "    static int staticPadBR" + filler + " = 0;"
                + "    static int staticPadBS" + filler + " = 0;"
                + "    static int staticPadBT" + filler + " = 0;"
                + " "
                + "    static int getStaticB() {"
                + "         return intFieldB" + filler + ";"
                + "     }"
                + " "
                + "    static void setStaticB(int x) {"
                + "         intFieldB" + filler + " = x;"
                + "     }"
                + " "
                + "    static int intFieldC" + filler + " = 0;"
                + "    static int staticPadCA" + filler + " = 0;"
                + "    static int staticPadCB" + filler + " = 0;"
                + "    static int staticPadCC" + filler + " = 0;"
                + "    static int staticPadCD" + filler + " = 0;"
                + "    static int staticPadCE" + filler + " = 0;"
                + "    static int staticPadCF" + filler + " = 0;"
                + "    static int staticPadCG" + filler + " = 0;"
                + "    static int staticPadCH" + filler + " = 0;"
                + "    static int staticPadCI" + filler + " = 0;"
                + "    static int staticPadCJ" + filler + " = 0;"
                + "    static int staticPadCK" + filler + " = 0;"
                + "    static int staticPadCL" + filler + " = 0;"
                + "    static int staticPadCM" + filler + " = 0;"
                + "    static int staticPadCN" + filler + " = 0;"
                + "    static int staticPadCO" + filler + " = 0;"
                + "    static int staticPadCP" + filler + " = 0;"
                + "    static int staticPadCQ" + filler + " = 0;"
                + "    static int staticPadCR" + filler + " = 0;"
                + "    static int staticPadCS" + filler + " = 0;"
                + "    static int staticPadCT" + filler + " = 0;"
                + " "
                + "    static int getStaticC() {"
                + "        return intFieldC" + filler + ";"
                + "    }"
                + " "
                + "    static void setStaticC(int x) {"
                + "        intFieldC" + filler + " = x;"
                + "    }"
                + " "
                + "    static int intFieldD" + filler + " = 0;"
                + " "
                + "    static int getStaticD() {"
                + "        return intFieldD" + filler + ";"
                + "    }"
                + " "
                + "    static void setStaticD(int x) {"
                + "        intFieldD" + filler + " = x;"
                + "    }"
                + " "
                + "    int instA" + filler + " = 0;"
                + " "
                + "    int getA() {"
                + "        return instA" + filler + ";"
                + "    }"
                + " "
                + "    void setA(int x) {"
                + "        instA" + filler + "= x;"
                + "    }"
                + " "
                + "    int padAA" + filler + " = 0;"
                + "    int padAB" + filler + " = 0;"
                + "    int padAC" + filler + " = 0;"
                + "    int padAD" + filler + " = 0;"
                + "    int padAE" + filler + " = 0;"
                + "    int padAF" + filler + " = 0;"
                + "    int padAG" + filler + " = 0;"
                + "    int padAH" + filler + " = 0;"
                + "    int padAI" + filler + " = 0;"
                + "    int padAJ" + filler + " = 0;"
                + "    int padAK" + filler + " = 0;"
                + "    int padAL" + filler + " = 0;"
                + "    int padAM" + filler + " = 0;"
                + "    int padAN" + filler + " = 0;"
                + "    int padAO" + filler + " = 0;"
                + "    int padAP" + filler + " = 0;"
                + "    int padAQ" + filler + " = 0;"
                + "    int padAR" + filler + " = 0;"
                + "    int padAS" + filler + " = 0;"
                + "    int padAT" + filler + " = 0;"
                + " "
                + "    int instB" + filler + " = 0;"
                + " "
                + "     int getB() {"
                + "         return instB" + filler + ";"
                + "     }"
                + " "
                + "     void setB(int x) {"
                + "         instB" + filler + "= x;"
                + "     }"
                + " "
                + "    int padBA" + filler + " = 0;"
                + "    int padBB" + filler + " = 0;"
                + "    int padBC" + filler + " = 0;"
                + "    int padBD" + filler + " = 0;"
                + "    int padBE" + filler + " = 0;"
                + "    int padBF" + filler + " = 0;"
                + "    int padBG" + filler + " = 0;"
                + "    int padBH" + filler + " = 0;"
                + "    int padBI" + filler + " = 0;"
                + "    int padBJ" + filler + " = 0;"
                + "    int padBK" + filler + " = 0;"
                + "    int padBL" + filler + " = 0;"
                + "    int padBM" + filler + " = 0;"
                + "    int padBN" + filler + " = 0;"
                + "    int padBO" + filler + " = 0;"
                + "    int padBP" + filler + " = 0;"
                + "    int padBQ" + filler + " = 0;"
                + "    int padBR" + filler + " = 0;"
                + "    int padBS" + filler + " = 0;"
                + "    int padBT" + filler + " = 0;"
                + " "
                + "    int instC" + filler + " = 0;"
                + " "
                + "    int getC() {"
                + "        return intFieldC" + filler + ";"
                + "    }"
                + " "
                + "    void setC(int x) {"
                + "        instC" + filler + "= x;"
                + "    }"
                + " "
                + "    int padCA" + filler + " = 0;"
                + "    int padCB" + filler + " = 0;"
                + "    int padCC" + filler + " = 0;"
                + "    int padCD" + filler + " = 0;"
                + "    int padCE" + filler + " = 0;"
                + "    int padCF" + filler + " = 0;"
                + "    int padCG" + filler + " = 0;"
                + "    int padCH" + filler + " = 0;"
                + "    int padCI" + filler + " = 0;"
                + "    int padCJ" + filler + " = 0;"
                + "    int padCK" + filler + " = 0;"
                + "    int padCL" + filler + " = 0;"
                + "    int padCM" + filler + " = 0;"
                + "    int padCN" + filler + " = 0;"
                + "    int padCO" + filler + " = 0;"
                + "    int padCP" + filler + " = 0;"
                + "    int padCQ" + filler + " = 0;"
                + "    int padCR" + filler + " = 0;"
                + "    int padCS" + filler + " = 0;"
                + "    int padCT" + filler + " = 0;"
                + " "
                + "    int instD" + filler + " = 0;"
                + " "
                + "    int  getD() {"
                + "        return intFieldD" + filler + ";"
                + "    }"
                + " "
                + "    void setD(int x) {"
                + "        instD" + filler + "= x;"
                + "    }"
                + " "
                + "    int padDA" + filler + " = 0;"
                + "    int padDB" + filler + " = 0;"
                + "    int padDC" + filler + " = 0;"
                + "    int padDD" + filler + " = 0;"
                + "    int padDE" + filler + " = 0;"
                + "    int padDF" + filler + " = 0;"
                + "    int padDG" + filler + " = 0;"
                + "    int padDH" + filler + " = 0;"
                + "    int padDI" + filler + " = 0;"
                + "    int padDJ" + filler + " = 0;"
                + "    int padDK" + filler + " = 0;"
                + "    int padDL" + filler + " = 0;"
                + "    int padDM" + filler + " = 0;"
                + "    int padDN" + filler + " = 0;"
                + "    int padDO" + filler + " = 0;"
                + "    int padDP" + filler + " = 0;"
                + "    int padDQ" + filler + " = 0;"
                + "    int padDR" + filler + " = 0;"
                + "    int padDS" + filler + " = 0;"
                + "    int padDT" + filler + " = 0;"
                + " "
                + " "
                + "    volatile MethodHandle targetMethod = null;"
                + " "
                + "    public void setMethod( MethodHandle m) {"
                + "        targetMethod = m;"
                + "    }"
                + " "
                + " "
                + "    public static Integer decrement" + filler + "( Integer d) {"
                + "        return --d;"
                + "    }"
                + " "
                + " "
                + "    static ToIntFunction<Integer> next" + filler + " = d -> decrement" + filler + "(d);"
                + " "
                + " "
                + newLine
                + "    public Integer get( Integer depth) throws Throwable {"
                + newLine
                + "        if (depth > 0) {"
                + newLine
                + "            Integer newDepth =  next" + filler + ".applyAsInt( --depth );"
                + "            return  get2" + filler + "( newDepth);"
                + newLine
                + "        } else {"
                + "            return  getA() + getMyId();"
                + "        }"
                + "    }"
                + " "
                + "    public Integer get2" + filler + "( Integer depth) throws Throwable {"
                + "        if (depth > 0 ) {"
                + "            Integer newDepth =  next" + filler + ".applyAsInt( --depth );"
                + "            return  getB();"
                + "        } else {"
                + "            return  getB();"
                + "        }"
                + "    }"
                + "}";
    }


    class BenchLoader extends ClassLoader {

        BenchLoader() {
            super();
        }

        BenchLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals(classNames[index])) {
                assert compiledClasses[index] != null;
                return defineClass(name, compiledClasses[index],
                        0,
                        (compiledClasses[index]).length);
            } else {
                return super.findClass(name);
            }
        }
    }

    MethodHandleStress.BenchLoader loader1 = new MethodHandleStress.BenchLoader();
    static MethodType setMethodType = MethodType.methodType(void.class, MethodHandle.class);
       static  MethodType generatedGetType = MethodType.methodType(Integer.class, Integer.class);

    @Setup(Level.Trial)
    public void setupClasses() throws Exception {
        Object[] receivers1;

        compiledClasses = new byte[classes][];
        loadedClasses = new Class[classes];
        classNames = new String[classes];

        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();

        for (int i = 0; i < classes; i++) {
            classNames[i] = "B" + i;
            compiledClasses[i] = InMemoryJavaCompiler.compile(classNames[i], B(i,nextText(25).intern()));
        }

        for (index = 0; index < compiledClasses.length; index++) {
            Class<?> c = loader1.findClass(classNames[index]);
            loadedClasses[index] = c;

            Constructor<?>[] ca = c.getConstructors();
            assert ca.length == 1;

            // Build the list of prebind MHs
            ConcurrentHashMap<Object, MethodHandle> prebinds = new ConcurrentHashMap<>();

            receivers1 = new Object[instances];
            for (int j = 0; j < instances; j++) {
                Object inst= ca[0].newInstance();
                receivers1[j] = inst;
                MethodHandle mh = publicLookup.findVirtual(c, methodNames[0], generatedGetType);
                mh = mh.bindTo(inst);
                prebinds.put(inst, mh);

            }
            instancesOfClassMap.put(c, receivers1);
            prebindMethods.put(c, prebinds);
        }

        IntStream.range(0, compiledClasses.length).parallel().forEach(c -> {
            IntStream.range(0, instances).forEach(x -> {
                try {
                    // Get the instance we are going to set
                    Class currClass = loadedClasses[c];
                    assert currClass != null : "No class? " + c;

                    Object currObj = ((Object[]) instancesOfClassMap.get(currClass))[x];
                    assert currObj != null : "No instance of " + currClass + " at " + x;

                    // For each instance of class C
                    //  choose a random class
                    Class rClass = chooseClass();
                    assert rClass != null;
                    //  choose a random instance of that class
                    Object rObj = chooseInstance(rClass);
                    assert rObj != null;

                    MethodHandle mh2 = publicLookup.findVirtual(currClass, "setMethod", setMethodType);

                    MethodHandle randomPrebindMethod = prebindMethods.get(rClass).get(rObj);
                    mh2.invoke(currObj,randomPrebindMethod);
                } catch (Throwable e) {
                    System.out.println("Exception = " + e);
                    e.printStackTrace();
                    System.exit(-1);
                }

            });
        });

        IntStream.range(0, compiledClasses.length).forEach(n -> {
                    IntStream.range(0, methodNames.length).forEach(m -> {
                        try {
                            IntStream.range(0, 1000).parallel().forEach(x -> {
                                try {
                                    executeOne();
                                } catch (Throwable e) {
                                }
                            });
                        } catch (Throwable e) {
                            System.out.println("Exception = " + e);
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    });
                });


        System.gc();
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    Class chooseClass() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int whichClass = tlr.nextInt(classes);
        return loadedClasses[whichClass];
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    Object chooseInstance(Class c) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int whichInst = tlr.nextInt(instances);
        return ((Object[]) instancesOfClassMap.get(c))[whichInst];
    }

    static final Integer recurse = new Integer(1);

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    Integer callTheMethod(MethodHandle m, Object r) throws Throwable {
        return (Integer) m.invokeExact( recurse );
    }

    int executeOne() throws Throwable {
        Class c = chooseClass();
        Object r = chooseInstance(c);
        MethodHandle m = prebindMethods.get(c).get(r);
        assert m != null;
        return callTheMethod(m, r);
    }

    @Benchmark
    @Fork(value = 3, jvmArgs = { "-XX:MetaspaceSize=1G", "-XX:MaxMetaspaceSize=1G",
            "-XX:ReservedCodeCacheSize=2g", "-XX:InitialCodeCacheSize=2g",
            "-Xmx2g", "-Xms2g" })
    public Integer work() throws Exception {
        int sum = 0;

        // Call a method of a random instance of a random class
        try {
            sum += executeOne();
        } catch (Throwable e) {
            System.out.println("Exception = " + e);
            e.printStackTrace();
        }
        return sum;
    }

}
