package app;

public class Starter {
    private String[] args;
    private static boolean quiet;

    public Starter(String[] args){
        this.args = args;
        this.quiet = false;
    }

    public void start() {
        int numThreads = 0;
        int precision = 0;

        String outFile = "pi";

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-p")) {
                precision = Integer.parseInt(args[i + 1]);
                i++;
            } else if (arg.equals("-t")) {
                numThreads = Integer.parseInt(args[i + 1]);
                i++;
            } else if (arg.equals("-o")) {
                outFile = args[i + 1];
                i++;
            } else if (arg.equals("-q")) {
                quiet = true;
            } else {
                System.out.println("Unknown option " + arg);
                System.exit(1);
            }
        }

        if (precision == 0) {
            System.out.println("Precision should be specified with -p");
            System.exit(1);
        }

        if (numThreads == 0) {
            numThreads = Runtime.getRuntime().availableProcessors();
            if(!quiet) {
                System.out.println("Threads used in current run: " + numThreads);
            }
        }

        try {
            Worker work = new Worker(numThreads, precision, outFile ,quiet);
            long executionTime = work.run();

            if(!quiet) {
                System.out.println("Threads used in current run: " + numThreads);
            }

            System.out.println("Total execution time for current run (millis): " +  executionTime);
        } catch (Exception ex) {
            System.out.println("Pi cannot be calculated - " + ex.getMessage());
        }

    }

}
