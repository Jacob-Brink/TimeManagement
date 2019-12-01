package Storage;

import Main.TimeManagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;

public class AsyncInsert implements Runnable {

    private BlockingQueue<SequentialRunnable> blockingQueue;

    public AsyncInsert(BlockingQueue<SequentialRunnable> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public void run() {

        while (true) {
            try {
                if (!this.blockingQueue.take().run()) break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
