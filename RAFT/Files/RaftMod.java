import edu.sjsu.cs249.raft.*;
import io.grpc.stub.StreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.IOException;
import java.util.HashMap;
import java.io.FileOutputStream;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongToIntFunction;
import java.util.logging.Logger;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.*;

public class RaftMod extends RaftServerGrpc.RaftServerImplBase {
    public static String votedFile = "/Users/mitesh/Desktop/SE/cs249/DistributedSystem/RAFT/Files/Voted";
    public static String ConfigFile = "/Users/mitesh/Desktop/SE/cs249/DistributedSystem/RAFT/Files/configFile", serverIp = "192.168.1.123";
    public static int serverPort;
    public  static Integer minElectionTimeout = 10000,  maxElectionTimeout = 15000, numofServers = 0, heartbeatTimeout = 5000;
    public static HashMap<Integer,String> configFileMap;
    public static HashMap<Integer,RaftServerGrpc.RaftServerBlockingStub> stubsMap;
    public static int lastLogIndex = 0, currIndex = 1;
    public static int lastLogTerm = 0;
    public static int leaderID;
    public static String lastdecree;

    public static int votedFor; //candidateId for which voted
    public static AtomicBoolean heartBeat = new AtomicBoolean(false);
    public static ExecutorService executorService;
    public static Integer electionTimeout;
    public static RaftMod raftMod;
    public static AtomicInteger  CURRENT_TERM= new AtomicInteger(0), COMMIT_INDEX;
    public  static int majoritycounter = 0;
    public static int SERVER_ID;
    //Timer Related
    public static Timer heartBeatTimer;
    public static Timer electionReqTimer;
    public  static TimerTask heartBeatTask;
    public static TimerTask electionReqTask;
    public static boolean isLeader = false;
    public static MongoClient mongoClient;
    public static DB db;
    public static DBCollection table;
    public static DataVoted dataVoted = new DataVoted(votedFile);

    public static class ElectionTimerTask extends TimerTask {
        @Override
        public void run() {
            System.out.println("Election Start Ping");
            startElection();
        }
    }
    public static class HeartBeatTimerTask extends TimerTask {
        @Override
        public void run() {
//            resetElectionTimer();
            System.out.println("HeartBeat Ping");
            System.out.println("I am the Leader "+ SERVER_ID);
            for (int key : stubsMap.keySet()) {
                try {
                    AppendEntriesResponse appendEntriesResponse = stubsMap.get(key).withDeadlineAfter(100,TimeUnit.MILLISECONDS).appendEntries(AppendEntriesRequest.newBuilder().setTerm(CURRENT_TERM.get()).build());

                } catch (Exception e) {

                }
            }

        }
    }

    //Constructor
    public RaftMod(String host, int port) throws  IOException {
        Server server = ServerBuilder.forPort(port).addService(this).build();
        server.start();
        System.out.println("Server started");
    }

    @Override
    public void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        //super.requestVote(request, responseObserver);
        System.out.println("Inside requestVote" + request.getTerm() + CURRENT_TERM.get());
        if (request.getTerm() < CURRENT_TERM.get()) {
            RequestVoteResponse voteResponse = RequestVoteResponse.newBuilder().setTerm(CURRENT_TERM.get()).setVoteGranted(false).build();
            responseObserver.onNext(voteResponse);
            responseObserver.onCompleted();
        } else if (request.getTerm() > CURRENT_TERM.get() && votedFor != request.getCadidateId()) {
            RequestVoteResponse voteResponse = RequestVoteResponse.newBuilder().setVoteGranted(true).setTerm(request.getTerm()).build();
            responseObserver.onNext(voteResponse);
            responseObserver.onCompleted();
            //CURRENT_TERM.set((int)request.getTerm());
            votedFor = request.getCadidateId();
            //TODO(Mitesh)-- WriteFile votedFor
            System.out.println("Response sent with Voted");
            resetElectionTimer();
            try {
//            dataVoted.FileWrite((int)request.getTerm(), request.getCadidateId());
            } catch ( Exception e) {
                System.out.println("Writing to file taking a lot of time");
            }
        }
    }

    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver)  {
        //If follower :
        //Mitesh -- Put more conditions here
        System.out.println("Inside appendEntries");
        if (request.getTerm() < CURRENT_TERM.get()) {
            AppendEntriesResponse appendEntriesResponse = AppendEntriesResponse.newBuilder().setTerm(CURRENT_TERM.get()).setSuccess(false).build();
            responseObserver.onNext(appendEntriesResponse);
            responseObserver.onCompleted();
        } else {
            //true -- HeartBest
            if (true) {
                System.out.println("Inside appendEntries Else");
                System.out.println("Inside appendEntries Else2");
                resetElectionTimer();
                leaderID = request.getLeaderId();
            } else {


            /*
            //TODO(Mitesh) -- When to update Log -- Revisit
            //If appendEntries but not HeartBeat
            lastLogIndex = (int)request.getPrevLogIndex();
            lastLogTerm = (int)request.getTerm();
            Entry entry = request.getEntry();
            int decree = Integer.valueOf(entry.getDecree());
            try {
                mongoDBWrite(lastLogTerm,lastLogIndex,decree);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            /*
            }
            lastLogIndex = (int) request.getPrevLogIndex();
            lastLogTerm = (int) request.getTerm();
            Entry entry = request.getEntry();
            updateLog(entry);
            */
            }

        }
    }

    public static void readConfigFile() throws IOException {
        File config = new File(ConfigFile);
        // ConfigFile
        System.out.println("Reading ConfigFile");
        BufferedReader br = new BufferedReader(new FileReader(config));
        String currLine = "";
        configFileMap = new HashMap<>();
        String currServer = serverIp + ":" + serverPort;
        //int serverCount = 0;
        while((currLine = br.readLine()) != null) {
            int candidateId = Integer.parseInt(String.valueOf(currLine.charAt(0)));
            String serverDetails = currLine.substring(2);
            numofServers++;
            if (!serverDetails.equalsIgnoreCase(currServer)) {
               configFileMap.put(candidateId,serverDetails);
            } else {
                SERVER_ID = candidateId;
            }
        }
        for (Map.Entry m: configFileMap.entrySet()) {
            System.out.println("configFileEnt "+m.getKey()+ " " + m.getValue());
        }
    }

    //createStubs
    private RaftServerGrpc.RaftServerBlockingStub getStub(String session) throws InterruptedException {
        InetSocketAddress addr = str2addr(session);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(addr.getHostName(), addr.getPort()).usePlaintext().build();
        return RaftServerGrpc.newBlockingStub(channel);
    }

    private static InetSocketAddress str2addr(String addr) {
        int colon = addr.lastIndexOf(':');
        return new InetSocketAddress(addr.substring(0, colon), Integer.parseInt(addr.substring(colon+1)));
    }

    public  void createStubs()  throws  InterruptedException{
        stubsMap = new HashMap<>();
        for (int currServerId: configFileMap.keySet()) {
            String currServerInfo = configFileMap.get(currServerId);
            RaftServerGrpc.RaftServerBlockingStub currStub = getStub(currServerInfo);
            stubsMap.put(currServerId,currStub);
        }

    }


    public  static  void startElection() {
        System.out.println("In startElection");
        CURRENT_TERM.getAndIncrement();
        for (int keys : stubsMap.keySet()) {
            try {
                RequestVoteResponse requestVoteResponse = stubsMap.get(keys).requestVote(RequestVoteRequest.newBuilder().setCadidateId(SERVER_ID).setTerm(CURRENT_TERM.get()).setLastLogIndex(lastLogIndex).setLastLogTerm(lastLogTerm).build());
                System.out.println("Mitesh :: Implement threadpool here");
                majoritycounter += 1;
            } catch ( Exception e) {
                System.out.println("Response Not coming from"+ keys);
            }

        }
        System.out.println("Counters: "+majoritycounter+ " " + numofServers);
        if (majoritycounter >= numofServers/2 ) {
            System.out.println("I am Leader--Yes");
            isLeader = true;
            stopElectionTimer();
            startHeartBeat();
        }
    }

    public static void startHeartBeat(){
            System.out.println("Starting HeartBeat");
            heartBeatTask = new HeartBeatTimerTask();
            heartBeatTimer.schedule(heartBeatTask,0,heartbeatTimeout);

    }

    public static void resetElectionTimer() {
        if (electionReqTask != null) {
            electionReqTask.cancel();
            electionReqTimer.purge();
        }
        electionReqTask = new ElectionTimerTask();
        Random r = new Random();
        electionTimeout = r.ints(minElectionTimeout,maxElectionTimeout).findFirst().getAsInt();
        System.out.println("Inside ResetElectionTimeout: " +electionTimeout);
        electionReqTimer.schedule(electionReqTask,electionTimeout);

    }

    public static void stopElectionTimer() {
        if (electionReqTask != null) {
            electionReqTask.cancel();
            electionReqTimer.purge();
        }

    }


    @Override
    public void clientAppend(ClientAppendRequest request, StreamObserver<ClientAppendResponse> responseObserver) {
        ClientAppendResponse response;
        if (!isLeader) {
            response = ClientAppendResponse.newBuilder().setRc(1).setIndex(currIndex).setLeader(leaderID).build();
        } else {
            currIndex += 1;
            lastLogIndex = currIndex;
            lastdecree = request.getDecree();
            //Update Log
            try {
                mongoDBWrite(CURRENT_TERM.get(),currIndex,request.getDecree());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            int count = 0;
            //Send RPCs to all nodes
            for (int key  : stubsMap.keySet()) {
                try {
                    AppendEntriesResponse appendEntriesResponse = stubsMap.get(key).appendEntries(AppendEntriesRequest.newBuilder().setTerm(CURRENT_TERM.get()).build());
                    count ++;
                } catch (Exception e) {
                }
            }

            if (count >= numofServers/2) {
                response = ClientAppendResponse.newBuilder().setRc(0).setIndex(currIndex).setLeader(SERVER_ID).build();
            } else {
                //TODO (Mitesh) -- What if majority does not respond
                response = ClientAppendResponse.newBuilder().setRc(0).setIndex(currIndex).setLeader(SERVER_ID).build();

            }
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void clientRequestIndex(ClientRequestIndexRequest request, StreamObserver<ClientRequestIndexResponse> responseObserver) {
        ClientRequestIndexResponse response;
        if (!isLeader) {
            response = ClientRequestIndexResponse.newBuilder().setIndex(lastLogIndex).setRc(1).setLeader(leaderID).setDecree(lastdecree).build();
        } else {
            String decree = getDecree((int)request.getIndex());
            int tmpIndex = (int)request.getIndex();
            String tmpDecree = decree;
            if (decree == ""){
                tmpIndex = lastLogIndex;
                tmpDecree = lastdecree;
            }
            response = ClientRequestIndexResponse.newBuilder().setIndex(tmpIndex).setRc(0).setLeader(leaderID).setDecree(tmpDecree).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    public void mongoDBWrite(int term, int index, String decree) throws UnknownHostException {
        // create a document to store key and value
        BasicDBObject document = new BasicDBObject();
//            document.put("id",1);
        document.put("term",term);
        document.put("index", index);
        document.put("decree", decree);
        table.insert(document);
        System.out.println("Inserted in MongoDB "+ term + index + decree);

    }

    public static void mongoDBRead() {
        /**** Find and display ****/
        BasicDBObject searchQuery = new BasicDBObject();
        DBCursor cursor = table.find();
        System.out.println("cursor is"+cursor);

        int maxIndex = -1;
        while (cursor.hasNext()) {
//                    System.out.println(cursor.next());
            DBObject dbobject = cursor.next();
            System.out.println(dbobject.get("term"));
            System.out.println(dbobject.get("index"));
            System.out.println(dbobject.get("decree"));
//          int  = (int)dbobject.get("term");
            int a = (int)dbobject.get("index");
            if (a > maxIndex) {
                maxIndex = a;
                lastLogIndex = a;
                lastLogTerm = (int)dbobject.get("term");
                lastdecree = (String)dbobject.get("decree");
            }
        }
    }


    public String getDecree(int index) {
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






    public static void main(String args[]) throws IOException,InterruptedException{
        //MongoDB connection for lastLogIndex, lastLogTerm and populate log and write log to DB
        mongoClient = new MongoClient("localhost", 27017);
        db = mongoClient.getDB("raftLog");
        table = db.getCollection("log");
        //Read Mongodb collection
        mongoDBRead();



        //Read DataVoted File to populate currentTerm and VotedFor
        ArrayList<Integer> arrayList = dataVoted.FileRead();
        if (arrayList.size() > 0) {
            CURRENT_TERM.set(arrayList.get(0));
        } else {
            CURRENT_TERM.set(0);
        }
        serverIp = args[0];
        serverPort = Integer.parseInt(args[1]);
//        int serverPort = 1111;


        readConfigFile();
        System.out.println("CURRENT_TERM "+CURRENT_TERM.get());
        System.out.println("NumofServers "+numofServers);
        System.out.println("LastLogIndex "+lastLogIndex);
        System.out.println("LastLogterm "+ lastLogTerm);


        raftMod = new RaftMod(serverIp,serverPort);
        raftMod.createStubs();

        //Create Threads and send
        executorService = Executors.newFixedThreadPool(numofServers);
        Random r = new Random();
        electionTimeout = r.ints(minElectionTimeout,maxElectionTimeout).findFirst().getAsInt();
        System.out.println("Election Timeout # " + electionTimeout);
        // Use Timer/reminder thread here to become candidate and participate for Leader Election
//        electionTimeoutThread = new Thread();
        electionReqTimer = new Timer();
        heartBeatTimer = new Timer();
        electionReqTask = new ElectionTimerTask();
        electionReqTimer.schedule (electionReqTask,electionTimeout);

    }

}
