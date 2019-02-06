package org.wycliffeassociates.translationrecorder.Recording;

import org.wycliffeassociates.translationrecorder.AudioInfo;
//import org.wycliffeassociates.translationrecorder.Reporting.Logger;
import org.wycliffeassociates.translationrecorder.wav.WavFile;
import org.wycliffeassociates.translationrecorder.wav.WavOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;


public class WavFileWriter {

    public static final String KEY_WAV_FILE = "wavfile";
    private String nameWithoutExtension = null;
    public static int largest = 0;

    private void writeDataReceivedSoFar(FileOutputStream compressedFile, ArrayList<Byte> list, int increment, boolean stoppedRecording) throws IOException {
        byte[] data = new byte[increment];
        byte[] minAndMax = new byte[2 * AudioInfo.SIZE_OF_SHORT];
        //while there is more data in the arraylist than one increment
        while (list.size() >= increment) {
            //remove that data and put it in an array for min/max computation
            for (int i = 0; i < increment; i++) {
                data[i] = list.remove(0);
            }
            //write the min/max to the minAndMax array
            getMinAndMaxFromArray(data, minAndMax);
            //write the minAndMax array to the compressed file
            compressedFile.write(minAndMax);

        }
        //if the recording was stopped and there is less data than a full increment, grab the remaining data
        if (stoppedRecording) {
            System.out.println("Stopped recording, writing some remaining data");
            byte[] remaining = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                remaining[i] = list.remove(0);
            }
            getMinAndMaxFromArray(remaining, minAndMax);
            compressedFile.write(minAndMax);
        }
    }

    private void getMinAndMaxFromArray(byte[] data, byte[] minAndMax) {
        if (data.length < 4) {
            return;
        }
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int minIdx = 0;
        int maxIdx = 0;
        for (int j = 0; j < data.length; j += AudioInfo.SIZE_OF_SHORT) {
            if ((j + 1) < data.length) {
                byte low = data[j];
                byte hi = data[j + 1];
                short value = (short) (((hi << 8) & 0x0000FF00) | (low & 0x000000FF));
                if (max < value) {
                    max = value;
                    maxIdx = j;
                    if (value > largest) {
                        largest = value;
                    }
                }
                if (min > value) {
                    min = value;
                    minIdx = j;
                }
            }
        }
        minAndMax[0] = data[minIdx];
        minAndMax[1] = data[minIdx + 1];
        minAndMax[2] = data[maxIdx];
        minAndMax[3] = data[maxIdx + 1];
    }
}
