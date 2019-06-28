package app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatContext;

public class Worker {
    private int numThreads;
    private int precision;
    private String fileName;
    private boolean quiet;

    public Worker(int numThreads, int precision, String fileName, boolean quiet) {
        this.setNumThreads(numThreads);
        this.setPrecision(precision);
        this.setQuiet(quiet);
        this.setFileName(fileName);
    }

    public long run() throws NumberFormatException, IOException, ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();

        // ApfloatContext context = new ApfloatContext(new Properties());
        // context.setNumberOfProcessors(1);
        // ApfloatContext.setThreadContext(context);

        // ApfloatContext threadCtx = ApfloatContext.getThreadContext();
        // ApfloatContext ctx = (ApfloatContext) ApfloatContext.getContext().clone();
        // ctx.setNumberOfProcessors(this.getNumThreads());
        // ApfloatContext.setThreadContext(ctx);

        ChudnovskyAlgorithm algorithm = new ChudnovskyAlgorithm();
        Apfloat pi = algorithm.calculatePi(this.getPrecision(), this.getNumThreads(), this.isQuiet());

        long endTime = System.currentTimeMillis() - startTime;

        try {
            File output = new File(this.getFileName());
            if(!output.exists()) {
                output.createNewFile();
            }

            PrintWriter file = new PrintWriter(new FileOutputStream(output, false));
            file.println(pi.toString());

            file.flush();
            file.close();

        } catch (IOException ex) {
            System.err.println("Cannot write result to the file: " + this.getFileName() + " - " + ex.getMessage());
            return 0L;
        }

        return endTime;
    }

    private void setNumThreads(int numThreads) {
        this.numThreads = (numThreads < 0) ? 0 : numThreads;
    }

    private void setPrecision(int precision) {
        this.precision = (precision < 0) ? 0 : precision;
    }

    private void setFileName(String fileName) {
        this.fileName = ( fileName != null) ? fileName : "pi";
    }

    private void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    private int getNumThreads() {
        return numThreads;
    }

    private int getPrecision() {
        return precision;
    }

    private String getFileName() {
        return fileName;
    }

    private boolean isQuiet() {
        return quiet;
    }
}
