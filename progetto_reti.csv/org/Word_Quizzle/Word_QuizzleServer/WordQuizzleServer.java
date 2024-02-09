package org.Word_Quizzle.Word_QuizzleServer;

import org.Word_Quizzle.userTools.NoUserException;
import org.Word_Quizzle.userTools.User_Manager;
import org.Word_Quizzle.userTools.User_Session;
import org.Word_Quizzle.constantRMI.Constants;
import org.Word_Quizzle.constantRMI.Registration_Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

public class WordQuizzleServer {

    DatagramChannel UDPChannel;
    Registration_Server registrationServer;
    User_Manager manager;
    Selector selector;
    Map<String, Channel_suspended> waitingChannels;
    private boolean shuttingDownServer = false;//variabile che può essere modificata se si digita exit dalla CLI

    public WordQuizzleServer(){
        try {

            System.out.println("Benvenuti su Word_Quizzle Server");//stampo un messaggio di benvenuto

            this.manager = User_Manager.parseFromFile(new File("Database.json"));//recupero il database da file
            System.out.println("DATABASE IS READY");


            this.registrationServer = new Registration_Server(manager);//esporto il server rmi per il sign up e avvio il registry
            Registration_Service stub =
                    (Registration_Service) UnicastRemoteObject.exportObject(registrationServer, Constants.RMI_SERVER_PORT);
            LocateRegistry.createRegistry(1099).rebind("REGISTRATION_SERVICE", stub);
            System.out.println("RMI SERVER HAS STARTED");


            ServerSocketChannel loopbackServer = ServerSocketChannel.open();//creo il serverSocketChannel su localhost
            loopbackServer.socket().bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), Constants.TCP_SERVER_PORT));//mi metto in ascolto sulla porta 1536
            loopbackServer.configureBlocking(false);


            this.selector = Selector.open();//creo il selector e registro il socket
            loopbackServer.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("NIO SERVER STARTED ON ADDRESS "+InetAddress.getLoopbackAddress().toString());

            if(!InetAddress.getLocalHost().equals(InetAddress.getLoopbackAddress())){

                ServerSocketChannel localServer = ServerSocketChannel.open();//creo il serverSocketAddress sulla rete locale
                localServer.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), Constants.TCP_SERVER_PORT));//faccio una bind sulla porta 1536
                localServer.configureBlocking(false);//setto la blocking mode a false(Non blocking I/O)
                localServer.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("NIO SERVER STARTED ON "+InetAddress.getLocalHost().toString());
            }



            this.UDPChannel = DatagramChannel.open();//apro il datagram channel per mandare le richieste agli utenti sfidati


            this.waitingChannels = new HashMap<>();//set up della struttura dati usate per sospendere gli utenti

            (new Thread(new ConsoleLogService())).start();//thread che gestice le richieste CLI

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Listening_Started() {

        while(!shuttingDownServer){
            try {
                //Codice classico per l'attesa sul selector iterando sul SelectionKey


                selector.select();//mi metto in attesa bloccante su selector

                //servo i canali pronti
                Iterator<SelectionKey> readyKeys = selector.selectedKeys().iterator();
                while (readyKeys.hasNext()) {
                    SelectionKey key = readyKeys.next();
                    readyKeys.remove();


                    if (key.isAcceptable()) {//accetto la connessione e alloco  una nuova userSession
                        SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                        client.configureBlocking(false);
                        SelectionKey newKey = client.register(selector, SelectionKey.OP_READ);
                        newKey.attach(new User_Session((InetSocketAddress) client.getRemoteAddress()));
                    }


                    else if (key.isWritable()) {//scrivo la risposta giá elaborata, presente in responseBuffer[]
                        SocketChannel client = (SocketChannel) key.channel();
                        User_Session session = (User_Session) key.attachment();
                        System.out.println("invio: " + session.responseMessage);
                        if (session.responseBuffer[2].hasRemaining())
                            client.write(session.responseBuffer);
                        if (!session.responseBuffer[2].hasRemaining())
                            key.interestOps(SelectionKey.OP_READ);
                    }


                    else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        //leggo dal canale, se il cliente ha chiuso la connessione deregistro il canale
                        //se l'ultimo byte inviato é \n elaboro la richiesta
                        User_Session session = (User_Session) key.attachment();
                        int bytesRead = client.read(session.requestBuffer);
                        if (bytesRead == -1) {
                            if (session.nickname != null)
                                manager.set_offline_user(session.nickname);//deregistro il canale associato al nickname
                            session.delete();
                            client.close();
                        } else if (session.requestBuffer.get(session.requestBuffer.position() - 1) == 10) {   //ricevuto \n
                            executeRequest(key);//eseguo la richiesta da parte del client
                        } else {
                            //lettura ancora in corso, lascio il canale registrato per la lettura
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("server chiuso");
    }

    //esegui il parsing della richiesta, elaboro la risposta per le richieste non bloccanti,
    //mi sospendo altrimenti
    private void executeRequest(SelectionKey key) {
        User_Session session = (User_Session) key.attachment();
        ByteBuffer requestBuffer = session.requestBuffer.flip();
        byte[] array = new byte[requestBuffer.remaining()];
        requestBuffer.get(array).clear();
        String request = new String(array, StandardCharsets.UTF_8);
        System.out.println("ricevuto: "+request.trim()+" da "+session.nickname);

        String[] requestTokens = request.split(" ");
        try {
            switch (requestTokens[0]){
                case "LOGIN":
                    if(session.nickname != null)
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you already logged in");
                    else {
                        String nickname = requestTokens[1];
                        String password = requestTokens[2];
                        if (!manager.containsUser(nickname) || !manager.checkPassword(nickname, password))
                            session.putResponse(Constants.WRONG_CREDENTIAL_CODE, "wrong credential");
                        else{
                            session.nickname = nickname;
                            manager.setUserOnline(nickname, session.address);
                            session.putResponse(Constants.SUCCESS_CODE, "OK");
                        }
                    }
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;

                case "ADDFRIEND":
                    if(session.nickname == null)
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you have to log first");
                    else{
                        String friend = requestTokens[1];
                        if(friend.equals(session.nickname))
                            session.putResponse(Constants.FOREVER_ALONE_CODE, "You can't be a friend of yourself");
                        else{
                            manager.addFriendship(session.nickname, friend);
                            session.putResponse(Constants.SUCCESS_CODE, "OK");
                        }
                    }
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;

                case "SHOWFRIENDS":
                    if(session.nickname == null)
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you have to log first");
                    else
                        session.putResponse(Constants.SUCCESS_CODE, manager.showFriend(session.nickname));
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;

                case "SHOWRANKING":
                    if(session.nickname == null)
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you have to log first");
                    else
                        session.putResponse(Constants.SUCCESS_CODE, manager.showRanking(session.nickname));
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;

                case "SHOWSCORE":
                    if(session.nickname == null)
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you have to log first");
                    else
                        session.putResponse(Constants.SUCCESS_CODE, manager.showScore(session.nickname));
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;

                case "CHALLENGE":
                    if(session.nickname == null){
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you have to log first");
                        key.interestOps(SelectionKey.OP_WRITE);
                        break;
                    }
                    else {
                        String opponent = requestTokens[1];
                        if (!manager.isFriend(session.nickname, opponent)){
                            session.putResponse(Constants.NOT_FRIEND_CODE, "You're not friend with "+opponent);
                            key.interestOps(SelectionKey.OP_WRITE);
                            break;
                        }
                        else if(!manager.isOnline(opponent)){
                            session.putResponse(Constants.USER_UNAVAILABLE_CODE, opponent+" is offline");
                            key.interestOps(SelectionKey.OP_WRITE);
                            break;
                        }
                        else{
                            //richiesta bloccante, contatto opponent e mi sospendo
                            this.invitePlayer(session.nickname, opponent);
                            suspendChannel(key, 10000);
                            break;
                        }

                    }

                case "ACCEPTMATCH":
                    if(session.nickname == null){
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you have to log first");
                    }
                    else {
                        synchronized (this){
                            String challenger = requestTokens[1];
                            boolean hasChallengedAccepted = requestTokens[2].equals("YES");

                            if(!this.isChannelWaiting(challenger)){
                                //il timer ha giá risegliato challenger
                                session.putResponse(Constants.USER_UNAVAILABLE_CODE, "too late");
                            }
                            else if(hasWaitingUserDisconnected(challenger)){
                                //challenger si é disconnesso
                                session.putResponse(Constants.USER_UNAVAILABLE_CODE, "USER DISCONNETTED");
                            }
                            else {
                                //challenger é in attesa
                                User_Session challengerSession = restoreWaitingChannel(challenger);
                                if(hasChallengedAccepted) {
                                    Match match = new Match(challenger, session.nickname);
                                    session.setCurrentMatch(match);
                                    session.putResponse(Constants.SUCCESS_CODE, match.nextWord(session.nickname));
                                    challengerSession.setCurrentMatch(match);
                                    challengerSession.putResponse(Constants.SUCCESS_CODE, match.nextWord(challenger));
                                } else{
                                    session.putResponse(Constants.SUCCESS_CODE, "OK");
                                    challengerSession.putResponse(Constants.USER_UNAVAILABLE_CODE, session.nickname+" unavailable");
                                }
                            }
                        }
                    }
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;


                case "GUESSWORD":
                    if(session.nickname == null) {
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you have to log first");
                        key.interestOps(SelectionKey.OP_WRITE);
                    } else {
                        Match match = session.currentMatch;
                        String player = session.nickname;
                        String opponent = session.currentMatch.getOpponent(player);

                        match.putGuess(session.nickname, requestTokens[1]);
                        //player ha concluso
                        if (match.hasPlayerEnded(player)) {
                            //se é il primo a concludere si sospende, altrimenti calcola i risultati
                            if (match.hasPlayerEnded(opponent))
                                endMatch(key);
                            else
                                suspendChannel(key, 20000);
                        }
                        //player non ha concluso, prossima parola
                        else {
                            session.putResponse(Constants.NEW_WORD_CODE, match.nextWord(session.nickname));
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                    break;

                case "TIMEOUT":
                    if(session.nickname == null)
                        session.putResponse(Constants.INVALID_REQUEST_CODE, "you have to log first");
                    else {
                        Match match = session.currentMatch;
                        String player = session.nickname;
                        String opponent = session.currentMatch.getOpponent(player);

                        match.playerTimeout(player);
                        //se é il primo a concludere si sospende, altrimenti calcola i risultati
                        if (match.hasPlayerEnded(opponent))
                            endMatch(key);
                        else
                            suspendChannel(key, 20000);
                    }
                    break;

                default:
                    session.putResponse(Constants.BAD_FORMAT_CODE, "bad format");
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
            }

        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            session.putResponse(Constants.BAD_FORMAT_CODE, "bad format");
            key.interestOps(SelectionKey.OP_WRITE);
        } catch (NoUserException e) {
            session.putResponse(Constants.NON_EXISTENT_USER_CODE, "non existent user");
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private synchronized void endMatch(SelectionKey key){
        User_Session session = (User_Session) key.attachment();
        Match match = session.currentMatch;
        String player = session.nickname;
        String opponent = session.currentMatch.getOpponent(player);


        if(match.hasPlayerLeft(opponent)){//opponent ha lasciato durante la partita
            match.computeResult();
            session.putResponse(Constants.RESULT_CODE, match.getResult(session.nickname));
            session.unsetCurrentMatch();
            manager.updateUserScore(player, match.getScore(player));
            manager.updateUserScore(opponent, -10);//infliggo una penalità di 10 punti
        }

        else if(hasWaitingUserDisconnected(opponent)){//opponent ha lasciato mentre era sospeso
            match.playerLeaves(opponent);
            match.computeResult();
            session.putResponse(Constants.RESULT_CODE, match.getResult(session.nickname));
            session.unsetCurrentMatch();
            manager.updateUserScore(player, match.getScore(player));
            manager.updateUserScore(opponent, -10);//infliggo una penalità di 10 punti
        }
        //partita regolare
        else{
            User_Session opponentSession = restoreWaitingChannel(opponent);
            match.computeResult();
            session.putResponse(Constants.RESULT_CODE, match.getResult(session.nickname));
            session.unsetCurrentMatch();
            opponentSession.putResponse(Constants.RESULT_CODE, match.getResult(opponentSession.nickname));
            opponentSession.unsetCurrentMatch();
            manager.updateUserScore(player, match.getScore(player));
            manager.updateUserScore(opponent, match.getScore(opponent));
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void invitePlayer(String challenger, String challenged) {
        try {
            //invio il datagramam sullo stesso indirizzo e porta del socket TCP, avvalendomi del demultiplexing IP
            InetSocketAddress challengedSocketAddress = manager.getUser(challenged).socketAddress;
            ByteBuffer buffer = ByteBuffer.allocate(128);
            buffer.put(challenger.getBytes(StandardCharsets.UTF_8)).flip();
            this.UDPChannel.send(buffer, challengedSocketAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private class ConsoleLogService implements Runnable{
        @Override
        public void run() {
            while(!shuttingDownServer){
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    executeCLICommand(reader.readLine().trim());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void executeCLICommand(String command) throws IOException{
            //semplice CLI
            switch (command){
                case "show users"://mostro gli users contenuti nel Database.JSON
                    System.out.println(manager.writeString());
                    break;

                case "online clients":
                    System.out.println("Active clients: "); //utilizzo Le keys del selector per Recuperare le user sessio ed inviarle come attachment
                    selector.keys().stream().filter(key -> key.interestOps() != SelectionKey.OP_ACCEPT).forEach(
                            key -> {
                                try {
                                    User_Session session = (User_Session) key.attachment();
                                    SocketChannel client = (SocketChannel) key.channel();
                                    String address = client.getRemoteAddress().toString();
                                    String name = (session.nickname != null)?session.nickname:"unknown";
                                    System.out.printf("Client: %s\tName: %s\n", address, name);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                    System.out.println("Suspended clients: ");
                    synchronized (this){
                        waitingChannels.values().stream().forEach(
                                user -> {
                                    try {
                                        User_Session session = user.session;
                                        SocketChannel client = user.channel;
                                        String address = client.getRemoteAddress().toString();
                                        String name = (session.nickname != null)?session.nickname:"unknown";
                                        System.out.printf("Client: %s\tName: %s\n", address, name);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
                    }
                    break;


                case "exit":
                    manager.storeInFile(new File("Database.json"));
                    UnicastRemoteObject.unexportObject(registrationServer, true);
                    shuttingDownServer = true;
                    selector.wakeup();
                    break;

                case "help":
                    System.out.println("Possible commands: ");
                    Stream.of("online clients","show users", "exit","help").
                            forEach(System.out::println);
                    break;

                default:
                    System.out.println("unknown command");
                    break;
            }
        }
    }
// questi sono tutti i metodi synchronized citati nel progetto che operano sui canali in attesa
    private synchronized boolean isChannelWaiting(String username) {//funzione utlizzata per vedere se un channel è in attesa nella ACCEPTMATCH
        return this.waitingChannels.containsKey(username);
    }

    private synchronized boolean hasWaitingUserDisconnected(String username){//controlla se il canale è stato chiuso e se il client associato
        //allo username si fosse disconnesso
        return !this.waitingChannels.get(username).channel.isOpen();//utilizzata sempre in ACCEPTMATCH
    }

    private synchronized void suspendChannel(SelectionKey key, int timeout) {//funzione utilizzata per sospendere un canale
        String nickname = ((User_Session) key.attachment()).nickname;
        Channel_suspended channel = new Channel_suspended(key);
        this.waitingChannels.put(nickname, channel);
        key.cancel();
        Thread timer = new Thread(
                () -> {
                    try {Thread.sleep(timeout);} catch (InterruptedException e) {e.printStackTrace();}
                    this.timeOutChannel(channel);
                }
        );
        timer.start();
    }

    private synchronized User_Session restoreWaitingChannel(String username) { //funzione che serve a risvegliare un canale messo in attesa
        //non viene mai chiamato restore su un canale chiuso
        try {
            Channel_suspended channelSuspended = this.waitingChannels.remove(username);
            SelectionKey key = null;
            key = channelSuspended.channel.register(selector, SelectionKey.OP_WRITE);
            key.attach(channelSuspended.session);
            return channelSuspended.session;
        } catch (ClosedChannelException e) {
            e.printStackTrace();
            return null;
        }
    }

    private synchronized void timeOutChannel(Channel_suspended channelSuspended) {//Funzione utilizzata dopo che è avvenuto un timeout
        //funzione utilizzata nella chiamata di suspendChannel
        String username = channelSuspended.username;
        if(this.isChannelWaiting(username)){
            try {
                User_Session session = channelSuspended.session;
                this.waitingChannels.remove(username);
                SelectionKey key = channelSuspended.channel.register(selector, SelectionKey.OP_WRITE);
                key.attach(channelSuspended.session);
                if(session.currentMatch == null)    //sospeso dopo una CHALLENGE,quindi la risposta non è pervenuta in tempo
                    channelSuspended.session.putResponse(Constants.USER_UNAVAILABLE_CODE, "Player not available");
                else{                               //sospeso perché conclusa una partita e l'opponent ha lasciato
                    Match match = session.currentMatch;
                    String opponent = match.getOpponent(session.nickname);
                    match.playerLeaves(opponent);
                    match.computeResult();
                    session.putResponse(Constants.RESULT_CODE, match.getResult(session.nickname));
                    session.unsetCurrentMatch();
                    manager.updateUserScore(session.nickname, match.getScore(session.nickname));
                    manager.updateUserScore(opponent, -10);//l'opponent ha lasciato infliggo la penalità di 10 punti
                }
                this.selector.wakeup();
            } catch (ClosedChannelException e) {
                //l'utente si é disconnesso mentre era in attesa
            }
        }
        else{
            // il canale é giá stato ripristinato
        }
    }


}
