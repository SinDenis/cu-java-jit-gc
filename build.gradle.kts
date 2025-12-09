plugins {
    id("java")
    id("application")
}

group = "ru.sin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Default main class
application {
    mainClass.set(project.findProperty("mainClass") as String? ?: "ru.sin.Main")
}

// JVM arguments for detailed JIT output
val jitDebugArgs = listOf(
    "-XX:+PrintCompilation",
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+PrintInlining"
)

// Task for running with JIT debug flags
tasks.register<JavaExec>("runWithJitDebug") {
    group = "application"
    description = "Run application with JIT debug flags"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(project.findProperty("mainClass") as String? ?: "ru.sin.Main")
    jvmArgs = jitDebugArgs
}

// Individual tasks for each example
tasks.register<JavaExec>("runInlineExample") {
    group = "jit-examples"
    description = "Run Inline Optimization Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.InlineOptimizationExample")
}

tasks.register<JavaExec>("runInlineExampleDebug") {
    group = "jit-examples"
    description = "Run Inline Optimization Example with JIT debug output"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.InlineOptimizationExample")
    jvmArgs = jitDebugArgs
}

tasks.register<JavaExec>("runLoopExample") {
    group = "jit-examples"
    description = "Run Loop Optimization Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.LoopOptimizationExample")
}

tasks.register<JavaExec>("runLoopExampleDebug") {
    group = "jit-examples"
    description = "Run Loop Optimization Example with JIT debug output"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.LoopOptimizationExample")
    jvmArgs = jitDebugArgs
}

tasks.register<JavaExec>("runEscapeExample") {
    group = "jit-examples"
    description = "Run Escape Analysis Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.EscapeAnalysisExample")
}

tasks.register<JavaExec>("runEscapeExampleDebug") {
    group = "jit-examples"
    description = "Run Escape Analysis Example with JIT debug output"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.EscapeAnalysisExample")
    jvmArgs = listOf(
        "-XX:+PrintCompilation",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+PrintEliminateAllocations",
        "-XX:+PrintEscapeAnalysis"
    )
}

tasks.register<JavaExec>("runDeadCodeExample") {
    group = "jit-examples"
    description = "Run Dead Code Elimination Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.DeadCodeEliminationExample")
}

tasks.register<JavaExec>("runDeadCodeExampleDebug") {
    group = "jit-examples"
    description = "Run Dead Code Elimination Example with JIT debug output"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.DeadCodeEliminationExample")
    jvmArgs = jitDebugArgs
}

tasks.register<JavaExec>("runBranchExample") {
    group = "jit-examples"
    description = "Run Branch Prediction Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.BranchPredictionExample")
}

tasks.register<JavaExec>("runBranchExampleDebug") {
    group = "jit-examples"
    description = "Run Branch Prediction Example with JIT debug output"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.jit.BranchPredictionExample")
    jvmArgs = jitDebugArgs
}

// Task to run all examples
tasks.register("runAllExamples") {
    group = "jit-examples"
    description = "Run all JIT optimization examples"
    dependsOn(
        "runInlineExample",
        "runLoopExample",
        "runEscapeExample",
        "runDeadCodeExample",
        "runBranchExample"
    )
}

// ============================================================================
// GC and Memory Leak Examples
// ============================================================================

// Common GC monitoring arguments
val gcMonitoringArgs = listOf(
    "-Xlog:gc*:file=gc.log:time,level,tags",
    "-XX:+HeapDumpOnOutOfMemoryError",
    "-XX:HeapDumpPath=./heap_dumps/",
    "-XX:+PrintGCDetails",
    "-XX:+PrintGCDateStamps"
)

// Create heap_dumps directory
tasks.register("createHeapDumpDir") {
    group = "gc-examples"
    description = "Create heap_dumps directory"
    doLast {
        file("heap_dumps").mkdirs()
    }
}

// Static Collection Leak Examples
tasks.register<JavaExec>("runMemoryLeak") {
    group = "gc-examples"
    description = "Run Memory Leak Example (Static Collection Leak)"
    dependsOn("createHeapDumpDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.leak.MemoryLeakExample")
    jvmArgs = listOf(
        "-Xmx1g",
        "-Xms512m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./heap_dumps/heap_leak_oom.hprof"
    )
}

tasks.register<JavaExec>("runMemoryLeakSmallHeap") {
    group = "gc-examples"
    description = "Run Memory Leak Example with small heap (faster OOM)"
    dependsOn("createHeapDumpDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.leak.MemoryLeakExample")
    jvmArgs = listOf(
        "-Xmx512m",
        "-Xms256m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./heap_dumps/heap_leak_small_oom.hprof"
    )
}

tasks.register<JavaExec>("runMemoryLeakWithGCLogs") {
    group = "gc-examples"
    description = "Run Memory Leak Example with GC logging"
    dependsOn("createHeapDumpDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.leak.MemoryLeakExample")
    jvmArgs = listOf(
        "-Xmx1g",
        "-Xms512m",
        "-Xlog:gc*:file=./heap_dumps/gc_leak.log:time,level,tags",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./heap_dumps/heap_leak_oom.hprof"
    )
}

tasks.register<JavaExec>("runMemoryLeakFixed") {
    group = "gc-examples"
    description = "Run Memory Leak FIXED Example (no leak)"
    dependsOn("createHeapDumpDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.leak.MemoryLeakFixedExample")
    jvmArgs = listOf(
        "-Xmx1g",
        "-Xms512m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./heap_dumps/heap_fixed_oom.hprof"
    )
}

tasks.register<JavaExec>("runMemoryLeakFixedWithGCLogs") {
    group = "gc-examples"
    description = "Run Memory Leak FIXED Example with GC logging"
    dependsOn("createHeapDumpDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.leak.MemoryLeakFixedExample")
    jvmArgs = listOf(
        "-Xmx1g",
        "-Xms512m",
        "-Xlog:gc*:file=./heap_dumps/gc_fixed.log:time,level,tags",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./heap_dumps/heap_fixed_oom.hprof"
    )
}

// Listener Leak Examples
tasks.register<JavaExec>("runListenerLeak") {
    group = "gc-examples"
    description = "Run Listener Leak Example"
    dependsOn("createHeapDumpDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.leak.ListenerLeakExample")
    jvmArgs = listOf(
        "-Xmx1g",
        "-Xms512m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./heap_dumps/listener_leak_oom.hprof"
    )
}

tasks.register<JavaExec>("runListenerLeakSmallHeap") {
    group = "gc-examples"
    description = "Run Listener Leak Example with small heap"
    dependsOn("createHeapDumpDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.leak.ListenerLeakExample")
    jvmArgs = listOf(
        "-Xmx256m",
        "-Xms128m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./heap_dumps/listener_leak_small_oom.hprof"
    )
}

tasks.register<JavaExec>("runListenerLeakFixed") {
    group = "gc-examples"
    description = "Run Listener Leak FIXED Example"
    dependsOn("createHeapDumpDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.leak.ListenerLeakFixedExample")
    jvmArgs = listOf(
        "-Xmx1g",
        "-Xms512m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=./heap_dumps/listener_fixed_oom.hprof"
    )
}

// Task to run all GC examples
tasks.register("runAllGCExamples") {
    group = "gc-examples"
    description = "Run all GC examples (WARNING: will cause OOM!)"
    dependsOn(
        "runMemoryLeak",
        "runMemoryLeakFixed",
        "runListenerLeak",
        "runListenerLeakFixed"
    )
}

// ============================================================================
// GC Benchmark Examples - Compare different Garbage Collectors
// ============================================================================

// Common benchmark settings
val benchmarkHeap = "2g"
val benchmarkHeapLarge = "4g"

// GC configurations
val serialGCArgs = listOf("-XX:+UseSerialGC")
val parallelGCArgs = listOf("-XX:+UseParallelGC")
val g1GCArgs = listOf("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100")
val zgcArgs = listOf("-XX:+UseZGC")

// Create benchmark results directory
tasks.register("createBenchmarkDir") {
    group = "gc-benchmarks"
    description = "Create gc_benchmarks directory"
    doLast {
        file("gc_benchmarks").mkdirs()
    }
}

// ============================================================================
// Throughput Benchmark with different GCs
// ============================================================================

tasks.register<JavaExec>("runThroughputSerial") {
    group = "gc-benchmarks"
    description = "Throughput Benchmark with Serial GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.ThroughputBenchmark")
    jvmArgs = serialGCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runThroughputParallel") {
    group = "gc-benchmarks"
    description = "Throughput Benchmark with Parallel GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.ThroughputBenchmark")
    jvmArgs = parallelGCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runThroughputG1") {
    group = "gc-benchmarks"
    description = "Throughput Benchmark with G1 GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.ThroughputBenchmark")
    jvmArgs = g1GCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runThroughputZGC") {
    group = "gc-benchmarks"
    description = "Throughput Benchmark with ZGC (Java 15+)"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.ThroughputBenchmark")
    jvmArgs = zgcArgs + listOf("-Xmx$benchmarkHeapLarge", "-Xms$benchmarkHeapLarge")
}

// With GC logs
tasks.register<JavaExec>("runThroughputG1WithLogs") {
    group = "gc-benchmarks"
    description = "Throughput Benchmark with G1 GC and GC logging"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.ThroughputBenchmark")
    jvmArgs = g1GCArgs + listOf(
        "-Xmx$benchmarkHeap",
        "-Xms$benchmarkHeap",
        "-Xlog:gc*:file=./gc_benchmarks/throughput_g1_gc.log:time,level,tags"
    )
}

// ============================================================================
// Latency Benchmark with different GCs
// ============================================================================

tasks.register<JavaExec>("runLatencySerial") {
    group = "gc-benchmarks"
    description = "Latency Benchmark with Serial GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.LatencyBenchmark")
    jvmArgs = serialGCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runLatencyParallel") {
    group = "gc-benchmarks"
    description = "Latency Benchmark with Parallel GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.LatencyBenchmark")
    jvmArgs = parallelGCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runLatencyG1") {
    group = "gc-benchmarks"
    description = "Latency Benchmark with G1 GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.LatencyBenchmark")
    jvmArgs = g1GCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runLatencyZGC") {
    group = "gc-benchmarks"
    description = "Latency Benchmark with ZGC (Java 15+)"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.LatencyBenchmark")
    jvmArgs = zgcArgs + listOf("-Xmx$benchmarkHeapLarge", "-Xms$benchmarkHeapLarge")
}

tasks.register<JavaExec>("runLatencyZGCWithLogs") {
    group = "gc-benchmarks"
    description = "Latency Benchmark with ZGC and GC logging"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.LatencyBenchmark")
    jvmArgs = zgcArgs + listOf(
        "-Xmx$benchmarkHeapLarge",
        "-Xms$benchmarkHeapLarge",
        "-Xlog:gc*:file=./gc_benchmarks/latency_zgc_gc.log:time,level,tags"
    )
}

// ============================================================================
// Mixed Workload Benchmark with different GCs
// ============================================================================

tasks.register<JavaExec>("runMixedSerial") {
    group = "gc-benchmarks"
    description = "Mixed Workload Benchmark with Serial GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.MixedWorkloadBenchmark")
    jvmArgs = serialGCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runMixedParallel") {
    group = "gc-benchmarks"
    description = "Mixed Workload Benchmark with Parallel GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.MixedWorkloadBenchmark")
    jvmArgs = parallelGCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runMixedG1") {
    group = "gc-benchmarks"
    description = "Mixed Workload Benchmark with G1 GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.MixedWorkloadBenchmark")
    jvmArgs = g1GCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runMixedZGC") {
    group = "gc-benchmarks"
    description = "Mixed Workload Benchmark with ZGC (Java 15+)"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.MixedWorkloadBenchmark")
    jvmArgs = zgcArgs + listOf("-Xmx$benchmarkHeapLarge", "-Xms$benchmarkHeapLarge")
}

// ============================================================================
// Allocation Benchmark with different GCs
// ============================================================================

tasks.register<JavaExec>("runAllocationSerial") {
    group = "gc-benchmarks"
    description = "Allocation Benchmark with Serial GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.AllocationBenchmark")
    jvmArgs = serialGCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runAllocationParallel") {
    group = "gc-benchmarks"
    description = "Allocation Benchmark with Parallel GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.AllocationBenchmark")
    jvmArgs = parallelGCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runAllocationG1") {
    group = "gc-benchmarks"
    description = "Allocation Benchmark with G1 GC"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.AllocationBenchmark")
    jvmArgs = g1GCArgs + listOf("-Xmx$benchmarkHeap", "-Xms$benchmarkHeap")
}

tasks.register<JavaExec>("runAllocationZGC") {
    group = "gc-benchmarks"
    description = "Allocation Benchmark with ZGC (Java 15+)"
    dependsOn("createBenchmarkDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.gc.comparison.AllocationBenchmark")
    jvmArgs = zgcArgs + listOf("-Xmx$benchmarkHeapLarge", "-Xms$benchmarkHeapLarge")
}

// Task to run all benchmarks
tasks.register("runAllBenchmarks") {
    group = "gc-benchmarks"
    description = "Run all GC benchmarks (takes time!)"
    dependsOn(
        "runThroughputG1",
        "runLatencyG1",
        "runMixedG1",
        "runAllocationG1"
    )
}

// ============================================================================
// Profiling Examples - async-profiler demonstrations
// ============================================================================

// Create profiling results directory
tasks.register("createProfilingDir") {
    group = "profiling-examples"
    description = "Create profiling_results directory"
    doLast {
        file("profiling_results").mkdirs()
    }
}

// Slow application example (before optimization)
tasks.register<JavaExec>("runSlowApp") {
    group = "profiling-examples"
    description = "Run slow (unoptimized) application for profiling"
    dependsOn("createProfilingDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.profiling.SlowApplicationExample")
    // Standard heap for profiling
    jvmArgs = listOf("-Xmx2g", "-Xms2g")
}

// Optimized application example (after optimization)
tasks.register<JavaExec>("runOptimizedApp") {
    group = "profiling-examples"
    description = "Run optimized application to compare performance"
    dependsOn("createProfilingDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.profiling.OptimizedApplicationExample")
    jvmArgs = listOf("-Xmx2g", "-Xms2g")
}

// Run slow app with Java Flight Recorder
tasks.register<JavaExec>("runSlowAppWithJFR") {
    group = "profiling-examples"
    description = "Run slow app with Java Flight Recorder enabled"
    dependsOn("createProfilingDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.profiling.SlowApplicationExample")
    jvmArgs = listOf(
        "-Xmx2g",
        "-Xms2g",
        "-XX:StartFlightRecording=duration=60s,filename=./profiling_results/slow_app.jfr",
        "-XX:FlightRecorderOptions=stackdepth=256"
    )
}

// Run optimized app with Java Flight Recorder
tasks.register<JavaExec>("runOptimizedAppWithJFR") {
    group = "profiling-examples"
    description = "Run optimized app with Java Flight Recorder enabled"
    dependsOn("createProfilingDir")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.sin.profiling.OptimizedApplicationExample")
    jvmArgs = listOf(
        "-Xmx2g",
        "-Xms2g",
        "-XX:StartFlightRecording=duration=60s,filename=./profiling_results/optimized_app.jfr",
        "-XX:FlightRecorderOptions=stackdepth=256"
    )
}