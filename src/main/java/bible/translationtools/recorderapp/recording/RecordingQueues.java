package bible.translationtools.recorderapp.recording;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by jsarabia on 7/24/15.
 */
public class RecordingQueues {
    public static BlockingQueue<RecordingMessage> UIQueue = new ArrayBlockingQueue<>(32768);
    public static BlockingQueue<RecordingMessage> writingQueue = new ArrayBlockingQueue<>(32768);
    public static BlockingQueue<RecordingMessage> compressionQueue = new ArrayBlockingQueue<>(32768);
    public static BlockingQueue<Boolean> doneWriting = new ArrayBlockingQueue<>(1);
    public static BlockingQueue<Boolean> doneWritingCompressed = new ArrayBlockingQueue<>(1);
    public static BlockingQueue<Boolean> doneUI = new ArrayBlockingQueue<>(1);

    public static void pauseQueues(){
        try {
            RecordingQueues.writingQueue.put(new RecordingMessage(null, true, false));
            RecordingQueues.compressionQueue.put(new RecordingMessage(null, true, false));
            RecordingQueues.UIQueue.put(new RecordingMessage(null, true, false));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void stopVolumeTest(){
        try {
            //Signal the threads reading from the Queues to stop
            RecordingQueues.UIQueue.put(new RecordingMessage(null, false, true));

            //Block until the threads are done
            Boolean done = RecordingQueues.doneUI.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void stopQueues(){
        try {
            //Signal the threads reading from the Queues to stop
            RecordingQueues.UIQueue.put(new RecordingMessage(null, false, true));
            RecordingQueues.writingQueue.put(new RecordingMessage(null, false, true));
            RecordingQueues.compressionQueue.put(new RecordingMessage(null, false, true));

            //Block until the threads are done
            Boolean done = RecordingQueues.doneWriting.take();
            Boolean done2 = RecordingQueues.doneWritingCompressed.take();
            Boolean done3 = RecordingQueues.doneUI.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void clearQueues(){
        RecordingQueues.writingQueue.clear();
        RecordingQueues.compressionQueue.clear();
    }

    private RecordingQueues(){}


}
