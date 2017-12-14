package gyroreader.nn.com.androidgyroscope;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class FileUtil {
    private static final String ALBUM_NAME = "AndroidDistance";
    private static final String FILENAME_ROOT = "execution_";

    private File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                ALBUM_NAME);
        if (!file.mkdirs()) {
            Log.e("Error", "Directory not created");
        }
        return file;
    }

    public void saveData(float[] sensorValues) {
        Date date = new Date();
        String value = "";
        for (int i = 0; i < sensorValues.length; i++) {
            value = value + sensorValues[i] + ",";
        }
        try {
            File dir = getAlbumStorageDir();
            File file = new File(dir, FILENAME_ROOT + System.currentTimeMillis() + ".csv");
            FileWriter writer = new FileWriter(file, true);
            BufferedWriter output = new BufferedWriter(writer);
            output.append(value + date.getTime() + "\n");
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
