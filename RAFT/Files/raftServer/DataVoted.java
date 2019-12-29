import java.io.BufferedWriter;
import java.io.*;
import java.io.FileInputStream;
import java.util.ArrayList;

public class DataVoted {
    public static String file = "/Users/mitesh/Desktop/SE/cs249/DistributedSystem/RAFT/Voted";
    public static ArrayList<Integer> arrayList;

    public DataVoted (String  file) {
        this.file  = file;
    }

    public static void FileWrite(int a, int b) {
        try{

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            writer.write(String.valueOf(a));
            writer.newLine();
            writer.write(String.valueOf(b));
            writer.flush();
            System.out.println("success...");
        } catch(Exception e) {
            System.out.println("Not able to write");
        }

    }

    public static ArrayList<Integer> FileRead() {
        arrayList = new ArrayList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();

            if (line == null) {
                arrayList.add(0);
                arrayList.add(0);
                return arrayList;

            }
            while (line != null) {
                int val = Integer.valueOf(line);
                System.out.println(val);
                // read next line
                arrayList.add(val);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            arrayList.add(0);
            arrayList.add(0);
        }


        return arrayList;
    }
    public static void main(String args[]) {

        DataVoted dataVoted = new DataVoted("/Users/mitesh/Desktop/SE/cs249/DistributedSystem/RAFT/Voted");
        FileWrite(25,6);
        arrayList = FileRead();
    }
}

