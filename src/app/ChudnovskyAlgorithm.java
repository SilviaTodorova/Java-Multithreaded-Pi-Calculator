package app;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatMath;
import org.apfloat.ApintMath;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class ChudnovskyAlgorithm {
    private List<Pair<Apfloat, Apfloat>> sum;
    private static Apfloat C;
    private static Apfloat C3_OVER_24;

    public ChudnovskyAlgorithm() {
        this.C = new Apfloat(640320l);
        this.sum = new ArrayList<Pair<Apfloat, Apfloat>>();
    }

    public Apfloat calculatePi(final long precision, int numberOfThreads, boolean quiet) {
        List<Range> ranges = ChudnovskyAlgorithm.calculateTermRanges(numberOfThreads, precision);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        final List<Callable<Void>> tasks = new LinkedList<>();

        for (final Range r : ranges) {
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    long startTime = 0;

                    ApfloatContext context = new ApfloatContext(new Properties());
                    context.setNumberOfProcessors(1);
                    ApfloatContext.setThreadContext(context);

                    if(!quiet) {
                        System.out.println("Thread: "+Thread.currentThread().getName()+" started.");
                        startTime = System.currentTimeMillis();
                    }
                    Pair<Apfloat, Apfloat> result = ChudnovskyAlgorithm.calculateTermSums(r, precision);

                    synchronized (tasks) {
                        sum.add(result);
                    }

                    if(!quiet) {
                        System.out.println("Thread: "+Thread.currentThread().getName()+" stopped.");
                        System.out.println("Thread: "+Thread.currentThread().getName()+" execution time was (millis): " + (System.currentTimeMillis() - startTime));
                    }

                    return null;
                }});

        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        finally {
            executor.shutdown();
        }

        return ChudnovskyAlgorithm.merge(sum, precision);
    }

    private static List<Range> calculateTermRanges(long numberOfRanges, long precision) {
        if (numberOfRanges <= 0) {
            throw new IllegalArgumentException("Number of ranges should be positive.");
        }

        C3_OVER_24 = (C.multiply(C).multiply(C)).divide(new Apfloat(24, precision));

        double chudnovskyConvergenceRate = Math.log(151931373056000L)/Math.log(10);
        Long numberOfTerms = (long)Math.ceil(precision / chudnovskyConvergenceRate);
        List<Range> ranges = new ArrayList<Range>();

        long start = 0;
        long end = numberOfTerms;

        if(end <= numberOfRanges) {
            int i = 0;
            int step=1;
            if(end % 2 ==0) {
                step=2;
            }

            while(i < end) {
                ranges.add(new Range(i,i+step));
                i = i + step + 1;
                step=1;
            }

        } else {
            long difference = (end - start) / numberOfRanges;
            long value = start;
            long resident = numberOfTerms % difference;

            int i = 1;
            while (value < end) {
                start = value;
                value += difference + resident;
                resident = 0;

                if(value <= numberOfTerms) {
                    ranges.add(new Range(start,value));
                } else {
                    int x =0;
                }

                i++;
            }
        }

        return ranges;
    }

    private static Pair<Apfloat, Apfloat> calculateTermSums(Range range, long precision) {
        // need one extra place for the 3
        precision = precision + 2;

        Apfloat negativeOne = new Apfloat(-1l);
        Apfloat two = new Apfloat(2l);
        Apfloat three = new Apfloat(3l);
        Apfloat five = new Apfloat(5l);
        Apfloat six = new Apfloat(6l);

        // Find the first term in the series
        Apfloat k = new Apfloat(range.initalK);
        Apfloat sign = Apfloat.ONE;
        if(k.longValue() % 2 != 0) {
            sign = negativeOne;
        }

        //sign * (6k)!
        // Need to push out the precision in this term
        // by a bit for the division to work properly.
        // 8% is probably too high, but should be a safe estimate
        Apfloat numerator = sign.multiply(ApintMath.factorial(6 * k.longValue())).precision((long) (precision * 1.08));

        //(k)!
        Apfloat kFactorial = ApintMath.factorial(k.longValue());
        //(3k!)*(k!)*(k!)*(k!)*constant^(3k)
        Apfloat denominator = ApintMath.factorial(three.multiply(k).longValue()).multiply(kFactorial.multiply(kFactorial).multiply(kFactorial)).multiply(ApfloatMath.pow(C, k.longValue() * 3));

        Apfloat a_k = numerator.divide(denominator);

        Apfloat a_sum = new Apfloat(0l).add(a_k);
        Apfloat b_sum = new Apfloat(0l).add(k.multiply(a_k));

        k = k.add(Apfloat.ONE);
        for (long i = range.initalK + 1; i < range.finalK; i++) {
            // a_k * sign * (6k -5)*(6k-3)*(6k-2)*(6k-1)*6k
            numerator = a_k.multiply(negativeOne.multiply((six.multiply(k).subtract(five)).multiply(two.multiply(k).subtract(Apfloat.ONE)).multiply(six.multiply(k).subtract(Apfloat.ONE))));

            // (k^3)*C3_OVER_24
            denominator = k.multiply(k).multiply(k).multiply(C3_OVER_24);

            a_k = numerator.divide(denominator);

            a_sum = a_sum.add(a_k);
            b_sum = b_sum.add(k.multiply(a_k));

            // Increase k with one
            k = k.add(Apfloat.ONE);
        }

        if (range.initalK == range.finalK) {
            a_sum = new Apfloat(0l);
            b_sum = new Apfloat(0l);
        }

        return new ImmutablePair<>(a_sum, b_sum);
    }

    private static Apfloat merge(List<Pair<Apfloat, Apfloat>> termSums, long precision) {
        Apfloat a_sum = new Apfloat(0l);
        Apfloat b_sum = new Apfloat(0l);

        for (Pair<Apfloat, Apfloat> termSum : termSums) {
            a_sum = a_sum.add(termSum.getLeft());
            b_sum = b_sum.add(termSum.getRight());
        }

        precision++;

        Apfloat sqrtTenThousandAndFive = ApfloatMath.sqrt(new Apfloat(10005l, precision + 1));
        Apfloat nominator = (new Apfloat(426880l)).multiply(sqrtTenThousandAndFive);
        Apfloat denominator = new Apfloat(13591409l).multiply(a_sum).add(new Apfloat(545140134l).multiply(b_sum));

        Apfloat pi = nominator.divide(denominator).precision(precision);

        return pi;
    }

}
