import java.util.Locale;
import java.util.SplittableRandom;

public class ParallelMonteCarloPi {

    private static final long ITERATIONS = 1_000_000_000L;
    private static final long SEED = 42L;

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            System.err.println("Usage: java ParallelMonteCarloPi <threads>");
            System.exit(1);
        }
        int threads;
        try {
            threads = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            threads = 0;
        }
        if (threads < 1) {
            System.err.println("The number of threads must be a positive integer.");
            System.exit(1);
        }

        long[] hits = new long[threads];
        Thread[] workers = new Thread[threads];

        long start = System.nanoTime();
        for (int t = 0; t < threads; t++) {
            // Spread the remainder so the per-thread counts add up to ITERATIONS exactly.
            long share = ITERATIONS / threads + (t < ITERATIONS % threads ? 1 : 0);
            int index = t;
            workers[t] = new Thread(() -> hits[index] = countHits(share, SEED + index));
            workers[t].start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

        long inside = 0;
        for (long h : hits) {
            inside += h;
        }
        double pi = 4.0 * inside / ITERATIONS;

        System.out.printf(Locale.US, "PI is %.5f%n", pi);
        System.out.printf(Locale.US, "THREADS %d%n", threads);
        System.out.printf(Locale.US, "ITERATIONS %,d%n", ITERATIONS);
        System.out.printf(Locale.US, "TIME %.2fms%n", elapsedMs);
    }

    // SplittableRandom is not thread-safe, so each thread gets its own instance;
    // it also avoids the lock contention a shared java.util.Random would cause.
    private static long countHits(long samples, long seed) {
        SplittableRandom random = new SplittableRandom(seed);
        long inside = 0; // local counter: writing to the shared array only once avoids false sharing
        for (long i = 0; i < samples; i++) {
            double x = random.nextDouble();
            double y = random.nextDouble();
            if (x * x + y * y <= 1.0) {
                inside++;
            }
        }
        return inside;
    }
}