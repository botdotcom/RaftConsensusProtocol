import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import com.mongodb.*;

public class MongoDBWrite {
     public static MongoClient mongo;
     public  static int lastLogIndex;

    static {
        try {
            mongo = new MongoClient("localhost", 27017);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**** Get database ****/
              // if database doesn't exists, MongoDB will create it for you
     public static  DB db = mongo.getDB("raftLog");
              /**** Get collection / table from 'testdb' ****/
              // if collection doesn't exists, MongoDB will create it for you
     public static DBCollection table = db.getCollection("log");




    public static  String getDecree(int index) {
          BasicDBObject searchQuery = new BasicDBObject("index",index);
          DBCursor cursor = table.find(searchQuery);
          String decree = "";
          while (cursor.hasNext()) {
              DBObject dbobject = cursor.next();
              System.out.println(dbobject.get("index"));
              if (index == (int)dbobject.get("index")) {
                  decree = (String) dbobject.get("decree");
              }

          }
          return decree;
    }


   public static void main(String[] args) throws UnknownHostException {
       /**** Connect to MongoDB ****/
       // Since 2.10.0, uses MongoClient

       BasicDBObject document = new BasicDBObject();
//       document.put("id",1);
       document.put("term",0);
       document.put("index", 0);
       document.put("decree", "UNKNOWN");
       table.insert(document);

       /**** Find and display ****/
       BasicDBObject searchQuery = new BasicDBObject();
       searchQuery.put("term", "1");

       // Read db's collection and populate hash
       DBCursor cursor = table.find();
       System.out.println("cursor is"+cursor);
       ArrayList<Integer> indexColl = new ArrayList<>();
       while (cursor.hasNext()) {
//               System.out.println(cursor.next());
           DBObject dbobject = cursor.next();
           System.out.println(dbobject.get("term"));
           System.out.println(dbobject.get("index"));
           System.out.println(dbobject.get("decree"));
           int a = (int)dbobject.get("index");
           System.out.println(a);
           indexColl.add(a);

       }
       Collections.sort(indexColl);
       lastLogIndex = indexColl.get(indexColl.size()-1);
       System.out.println("LastLogIndex"+lastLogIndex);

       String dec = getDecree(11);
       if (dec == "") {
           System.out.println("Decree Not Found");
       }
       System.out.println("Decree " + dec);

      /* Read DB and populate logHash*/

      }
}

