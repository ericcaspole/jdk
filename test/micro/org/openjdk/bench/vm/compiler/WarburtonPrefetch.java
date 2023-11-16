// See https://www.opsian.com/blog/jvms-allocateprefetch-options/
package org.openjdk.bench.vm.compiler;

import java.util.concurrent.TimeUnit;

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

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
@Fork(value = 2)
public class WarburtonPrefetch {

    class CacheSizedObj {

        public int p00, p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11;
    }

    class SmallObj {

        public int p01, p02, p03, p04, p05, p06, p07, p08, p09;
    }

    class LargeObj {

        public int p01, p02, p03, p04, p05, p06, p07, p08;
        public int p11, p12, p13, p14, p15, p16, p17, p18;
        public int p21, p22, p23, p24, p25, p26, p27, p28;
        public int p31, p32, p33, p34, p35, p36, p37, p38, p40;
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=0"})
    public CacheSizedObj allocateCacheAlignAPFS0() {
        return new CacheSizedObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=0"})
    public LargeObj allocateLargeObjAPFS0() {
        return new LargeObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=0"})
    public SmallObj allocateSmallObjAPFS0() {
        return new SmallObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=1"})
    public CacheSizedObj allocateCacheAlignAPFS1() {
        return new CacheSizedObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=1"})
    public LargeObj allocateLargeObjAPFS1() {
        return new LargeObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=1"})
    public SmallObj allocateSmallObjAPFS1() {
        return new SmallObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=2"})
    public CacheSizedObj allocateCacheAlignAPFS2() {
        return new CacheSizedObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=2"})
    public LargeObj allocateLargeObjAPFS2() {
        return new LargeObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=2"})
    public SmallObj allocateSmallObjAPFS2() {
        return new SmallObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=3"})
    public CacheSizedObj allocateCacheAlignAPFS3() {
        return new CacheSizedObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=3"})
    public LargeObj allocateLargeObjAPFS3() {
        return new LargeObj();
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:AllocatePrefetchStyle=3"})
    public SmallObj allocateSmallObjAPFS3() {
        return new SmallObj();
    }
}
