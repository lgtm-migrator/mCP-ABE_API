package com.mitu.utils;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

// TODO: Auto-generated Javadoc

/**
 * The Class Utility.
 *
 * @author mitu
 */
public class Utility {

    public static void copyAssets() {

//        AssetManager assetManager = PhrActivity.context.getAssets();
        String[] files = null;
        InputStream in = null;
        OutputStream out = null;
        String filename = "a.properties";
//        Log.e("SD", PhrActivity.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + filename);
        try {
//            in = assetManager.open(filename);
//            out = new FileOutputStream(PhrActivity.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + filename);
            assert in != null;
            copyFile(in, out);
            in.close();
//            in = null;
//            out.flush();
//            out.close();
//            out = null;
        } catch (IOException e) {
            System.err.println("tag Failed to copy asset file: " + filename + " " + e);
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }


    private static ArrayList<String> aRLs = new ArrayList<>();

	/* connect element of array with blank */

    /**
     * Array2 str.
     *
     * @param arr the arr
     * @return the string
     */
    public static String array2Str(String[] arr) {
        int len = arr.length;
        StringBuilder str = new StringBuilder(arr[0]);

        for (int i = 1; i < len; i++) {
            str.append(" ");
            str.append(arr[i]);
        }

        return str.toString();
    }

    /**
     * Println.
     *
     * @param o the o
     */
    public static void println(Object o, boolean debug) {
        if (debug) {
            System.out.println(o);
        }
    }

    public static void println(Object o) {
        System.out.println(o);
    }
    /**
     * Access file.
     *
     * @param filePath the file path
     * @param fileName the file name
     * @return the string
     */
    public static String accessFile(String filePath, String fileName) {
        File file = null;
        try {

            file = new File(filePath + fileName);
            if (file.createNewFile()) {
                Utility.println(fileName + " file is created!");
            } else {
                Utility.println(fileName + " file already exists.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    /**
     * @return the aRLs
     */
    public static ArrayList<String> getaRLs() {
        return aRLs;
    }

    /**
     * @param aRLs the aRLs to set
     */
    public static void setaRLs(ArrayList<String> aRLs) {
        Utility.aRLs = aRLs;
    }

    public static void serializeObject(String file, Object object) throws IOException {

        FileOutputStream fout = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(object);
        oos.close();
        fout.close();

    }

    public static ObjectInputStream deSerializeObject(String file) throws IOException {

        File dataFile = new File(file);

        FileInputStream fint = new FileInputStream(dataFile);

        return new ObjectInputStream(fint);

    }

    public static byte[] objectToByteArray(Object object) throws IOException {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ObjectOutput out = new ObjectOutputStream(bos)) {
                out.writeObject(object);
                return bos.toByteArray();
            }
        }
        // ignore close exception
        // ignore close exception
    }

    public static Object bytetArrayToObject(byte[] bytes) throws IOException, ClassNotFoundException {

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            try (ObjectInput in = new ObjectInputStream(bis)) {
                return in.readObject();
            }
        }
        // ignore close exception
        // ignore close exception
    }
}