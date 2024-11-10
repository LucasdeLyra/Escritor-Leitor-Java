import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class ReadersWritersProblem {
    private static final List<String> data = new ArrayList<>();
    private static final int NUM_THREADS = 100;
    private static final int NUM_ACCESSES = 100;

    private static int readCount = 0;
    private static final Semaphore mutex = new Semaphore(1);
    private static final Semaphore rwLock = new Semaphore(1);

    public static void main(String[] args) throws InterruptedException, IOException {
        loadData("bd.txt");
        List<MeasurementData> data = new ArrayList<>();

        for (int readerCount = 0; readerCount <= NUM_THREADS; readerCount++) {
            int writerCount = NUM_THREADS - readerCount;

            long totalTime = 0;
            for (int i = 0; i < 50; i++) {
                List<Thread> threads = createThreads(readerCount, writerCount);
                long startTime = System.currentTimeMillis();

                for (Thread thread : threads) {
                    thread.start();
                }
                for (Thread thread : threads) {
                    thread.join();
                }

                long endTime = System.currentTimeMillis();
                totalTime += (endTime - startTime);
            }

            double avgTime = totalTime / 50.0;
            data.add(new MeasurementData(readerCount, writerCount, avgTime));
        }
        for (MeasurementData dt: data){
            System.out.println(dt);
        }
    }

    private static void loadData(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        data.addAll(lines);
    }

    private static List<Thread> createThreads(int readerCount, int writerCount) {
        List<Thread> threads = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < readerCount; i++) {
            threads.add(new Thread(new Reader(data, random)));
        }
        for (int i = 0; i < writerCount; i++) {
            threads.add(new Thread(new Writer(data, random)));
        }
        Collections.shuffle(threads);
        return threads;
    }

    static class Reader implements Runnable {
        private final List<String> data;
        private final Random random;

        public Reader(List<String> data, Random random) {
            this.data = data;
            this.random = random;
        }
        public void lock(){
            try {
                mutex.acquire();
                readCount++;
                if (readCount == 1) {
                    rwLock.acquire();
                }
                mutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void unlock(){
            try{
                mutex.acquire();
                readCount--;
                if (readCount == 0) {
                    rwLock.release();
                }
                mutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            try {
                //lock(); //descomentar para ativar o modo padrão
                rwLock.acquire(); //descomentar para ativar o modo reader-writer
                for (int i = 0; i < NUM_ACCESSES; i++) {
                    int index = random.nextInt(data.size());
                    String word = data.get(index);  // Leitura                
                }
                Thread.sleep(1);
                //unlock(); //descomentar para ativar o modo padrão
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                rwLock.release(); //descomentar para ativar o modo reader-writer
            }
        }
    }

    static class Writer implements Runnable {
        private final List<String> data;
        private final Random random;

        public Writer(List<String> data, Random random) {
            this.data = data;
            this.random = random;
        }

        @Override
        public void run() {
            try {
                rwLock.acquire();
                for (int i = 0; i < NUM_ACCESSES; i++) {
                    int index = random.nextInt(data.size());
                    data.set(index, "MODIFICADO");  // Escrita
                }
                Thread.sleep(1);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                rwLock.release();
            }
        }
    }
    
    static class MeasurementData{
        public int readers;
        public int writers;
        public double avgTime;

        public MeasurementData(int readers, int writers, double avgTime){
            this.readers = readers;
            this.writers = writers;
            this.avgTime = avgTime;
        }

        @Override 
        public String toString() { 
            return readers + ";" + writers + ";" + avgTime; 
        } 
    }
}
