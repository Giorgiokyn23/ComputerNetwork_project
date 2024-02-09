package org.Word_Quizzle.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.Word_Quizzle.constantRMI.Constants;
import org.Word_Quizzle.constantRMI.Registration_Service;
import org.Word_Quizzle.constantRMI.Response;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;
import java.util.StringTokenizer;


public class WordQuizzleClient  {
    SocketChannel client;//dichiaro la SocketChannel
    Registration_Service registrationServer;//dichiaro l'interfaccia comune Registration Service implementata in constant RMi
    DatagramChannel UDPChannel;//dichiaro la porta UDP per le richieste di sfida
    ObjectMapper converter = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);//istanzio un ObjectMapper per la serializzazzione/deserializzazione
    public Scanner scanner=new Scanner(System.in);//istanzio uno scanner per la lettura da std in
    boolean newchallenge=false;//variabile utilizzata dl thread sulla porta UDP per vedere se c'è una nuova richiesta di sfida
    boolean timeout=false;//varabile utilizzata dal timer per il timeout
    private final Object lock= new Object();//lock utilizzata per leggere timeout in mutua esclusione
    private final Object lock1= new Object();//lock utilizzata per leggere newchallenge in mutua esclusione
    String sfidante; //nome dello sfidante
    boolean matchOn = false;//variabile per vedere se il client è impegnato in una sfida




    public  void elabora() {


        System.out.print("\n> ");
        String command = scanner.nextLine();//acquisisco un comando dallo standard input
        StringTokenizer st = new StringTokenizer(command); //StringTokenizer per il comando
        String nickUtente = null;


        synchronized (lock1){if (newchallenge){//controllo la variabile newchallenge


            switch (command){
                case "yes":
                    this.acceptMatch(sfidante);// se risposta affermativo invio una richiesta di ACCEPTMATCH al server

                    break;
                case "no":
                    NioTools.sendRequest(client, "ACCEPTMATCH", sfidante, "NO");// in caso negativo invio no
                    NioTools.Receive_Response(client);//ricevo la risposta dal server
                    break;
            }

        }
        }
        if(st.hasMoreTokens()) {
            String finalCommand = st.nextToken();//il primo token é il comando
            if(finalCommand.equals(Constants.cmd_sfida)) {
               this.startMatch(st.nextToken());// controllo che se fosse sfida invio una richiesta di CHALLENGE al server,dove il secondo token é il nome dello sfidante


            } else { //altrimenti
                switch(finalCommand) {
                    case Constants.ComHelp:
                        System.out.println(Constants.help);//stampo lo usage qualora fosse richiesto
                        break;
                    case Constants.cmd_login:
                        nickUtente = st.nextToken(); //il secondo  token è il nickUtente
                        this.Login(nickUtente,st.nextToken());

                        break;
                    case Constants.cmd_registra_utente:
                        nickUtente=st.nextToken();
                        doSignUp(nickUtente,st.nextToken());
                        break;
                    case Constants.cmd_logout: //logout
                        this.logout();

                        break;
                    case Constants.cmd_mostra_punteggio: //mostra_punteggio
                        this.showScore();
                        break;
                    case Constants.cmd_lista_amici: //lista_amici
                        this.showFriends();
                        break;
                    case Constants.cmd_mostra_classifica: //mostra_classifica
                        this.showLeaderboard();
                        break;
                    case Constants.cmd_aggiungi_amico: //aggiungi_amico

                        String amico = st.nextToken(); //l'amico è il secondo token
                        this.addFriend(amico);

                        break; //esco dal case aggiungi_amico

                }

            }
        }
    }


    public WordQuizzleClient(String serverAddress) {

        try {
            //setup del registration server via rmi
            registrationServer = (Registration_Service) LocateRegistry.getRegistry(serverAddress).lookup("REGISTRATION_SERVICE");



            //apro il client
            System.out.println("apro la connessione");
            client = SocketChannel.open(new InetSocketAddress(serverAddress, Constants.TCP_SERVER_PORT));


            UDPChannel = DatagramChannel.open();//apro la connessioone sulla porta UDP
            UDPChannel.bind(client.getLocalAddress());//eseguo una bind del SocketChannel sul local address


        } catch (IOException | NotBoundException e) {
            e.printStackTrace();

        }

    }



    //registro l'utente, e se ho successo tento il login
    private void doSignUp(String username, String password){
        try {
            if(NioTools.checkLength(username, password))
                if (registrationServer.registerUser(username, password))
                    Login(username, password);
                else

                 System.out.println("error username not available or too long");
        } catch (RemoteException ex) {
            ex.printStackTrace();

        }
    }


    private void Login(String username, String password){
        if(NioTools.checkLength(username, password)){
            NioTools.sendRequest(this.client, "LOGIN", username, password);//controllo li argomenti e se lunghezza giusta invio la richiesta
            Response response = NioTools.Receive_Response(this.client);//leggo la risposta dal server
            if(response.code != Constants.SUCCESS_CODE)

                System.out.println("error during login");
                else{


                (new Thread(new ChallengeAcceptanceService())).start(); //avvio un thread in ascolto sulla porta UDP


        }
        }
    }

    //chiudo il canale TCP e lo riapro
    private void logout(){
        if(matchOn){

            System.out.println("you can't log out during a match");
            return;
        }
        try{
            //chiudo la connessione precedente
            client.close();
            UDPChannel.close();

            //apro il client
            System.out.println("apro la connessione");
            client = SocketChannel.open(new InetSocketAddress("127.0.0.1", Constants.TCP_SERVER_PORT));

            //riapro l'udp sulla porta del nuovo client tcp
            UDPChannel = DatagramChannel.open();
            UDPChannel.bind(client.getLocalAddress());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addFriend(String friendName){
        if(matchOn){
            System.out.println("you can't add a friend during a match");
            return;
        }

        NioTools.sendRequest(this.client, "ADDFRIEND",  friendName);// invio la richiesta di ADDFRIEND al server
        Response response = NioTools.Receive_Response(this.client);//leggo la risposta e se non fosse uguale a SUCCES_CODE errore
        if(response.code != Constants.SUCCESS_CODE) System.out.println("error during the operation");
        else{
            System.out.println(friendName +" added with success");
        }
    }



    private void showFriends(){
        if(matchOn){
            System.out.println("you can't  see the friend list during a match");

            return;
        }
        try {
            NioTools.sendRequest(this.client, "SHOWFRIENDS");//stessa cosa che per ADDFRIEND
            Response response = NioTools.Receive_Response(this.client);
            ArrayNode friendList = converter.readValue(response.message, ArrayNode.class);//la risposta sarà un oggett JSON che convertirò con converter
            friendList.forEach((friend)->System.out.println(friend.textValue()));//per ogni nodo stampo il valore textVAlue ossia una stringa

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.out.println("error during the operation");
        }
    }

    private void showScore(){
        if(matchOn){

            System.out.println("you can't leave the match!");
            return;
        }
        try{
            NioTools.sendRequest(this.client, "SHOWSCORE");//stesso procedimento che per ADDFRIEND
            Response response = NioTools.Receive_Response(this.client);
            if(response.code != Constants.SUCCESS_CODE) {
                System.out.println("error during the operation");
            }
            else{

                int score = converter.readValue(response.message, ObjectNode.class).get("score").asInt();// stesso procedimento che per friendlist solo che score è un singolo campo
                System.out.println(("Your score: "+score));

            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();

        }
    }


    private void showLeaderboard(){
        if(matchOn){
            System.out.println("you can't leave the match!");
            return;
        }
        try{
            NioTools.sendRequest(this.client, "SHOWRANKING");//stesso cosa che per le richieste precedenti
            Response response = NioTools.Receive_Response(this.client);
            if(response.code != Constants.SUCCESS_CODE) {
                System.out.println("error during the operation");
            }
            else{

                ArrayNode scoreList = converter.readValue(response.message, ArrayNode.class);// stesso procedimento che per friendlist

                scoreList.forEach(
                        (user) -> System.out.println((user.get("nickname").asText()+": "+user.get("score")))//stampo il nickname e lo score per ogni user
                );//nota:lo user manager provede a fare sorting dei nodi JSON sul campo score
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.out.println("error during the operation");
        }
    }


    private void startMatch(String opponent){
        NioTools.sendRequest(this.client, "CHALLENGE", opponent);//invio la richiesta di CHALLENGE al server
        Response response = NioTools.Receive_Response(this.client);
        if(response.code != Constants.SUCCESS_CODE)

            System.out.println("error during the request of challenge");
        else{
            this.matchOn = true;

            playmatch(response);//Se la variabile matchOn è stata messa a true chiamo la funzione playmatch() dove il response passato sarà la prima parona

        }
    }


    private void acceptMatch(String challenger) { //chiamato quando si riceve un datagramma UDP
        NioTools.sendRequest(client, "ACCEPTMATCH", challenger, "YES");// qual'ora mi fosse stata mandata una richiesta di Challenge,eseguo la ACCEPTMATCH
        Response response = NioTools.Receive_Response(client);//ricevo la risposta che sarà la prima parola da tradurre
        switch (response.code){
            case Constants.USER_UNAVAILABLE_CODE:

                System.err.println("error"+Constants.Time_expired);
                break;

            case Constants.SUCCESS_CODE:
                this.matchOn = true;
                playmatch(response);//se ho avuto successo chiamo la funzione playmatch();
                break;

            default:

                System.err.println("error:"+Constants.Error_code);
        }
    }
private void playmatch(Response response ){
        Thread timerthread = new Thread(new Timer());//istanziio un nuov thread timer
        int i ;
        String nextword= response.message;//la prima parola da tradurre
        timerthread.start();// avvio il timer
        for(i=0;i<Constants.WordNumber;i++) {

            System.out.println("traduci la parola numero " +i+ ":" + " " + nextword);
            String answer= scanner.nextLine();//leggo la risposta dallo standard input
            synchronized (lock){
                if (timeout){//se avvenuto il timeout resetto le variabili di stato
                    timeout=false;
                    matchOn = false;
                    NioTools.sendRequest(client, "TIMEOUT");//invio la richiesta al server TIMEOUT
                    nextword= NioTools.Receive_Response(client).message;
                    break; }
            }

            NioTools.sendRequest(client, "GUESSWORD", answer);//invio le traduzioni al server
            nextword= NioTools.Receive_Response(client).message;//attendo la prossima parola o se avessi finito un messaggio di buona uscita

        }
    synchronized (lock) {
        matchOn = false;//il match è finito
    }
    System.out.println(nextword);//stampo le classifiche

}
private class Timer implements Runnable{

        @Override
        public void run(){
try {
            Thread.sleep(30000);//durata della partita

          synchronized (lock){

              if(matchOn){//scaduto il timer metto timeout=true;
                  timeout=true;
                  System.out.println(" tempo scaduto premere invio per continuare o attendi che l'avversario termini");
              }

          }

        }catch (InterruptedException e){
    e.printStackTrace();
}
    }
}







    private class ChallengeAcceptanceService implements Runnable {//in ascolto bloccante sulla porta udp

        public ChallengeAcceptanceService() {

        }
        @Override
        public void run() {
            while(true){
                try {

                    ByteBuffer buffer = ByteBuffer.allocate(128).clear();
                    UDPChannel.receive(buffer);//mi metto in attesa sulla porta udp


                    byte[] array = new byte[buffer.flip().remaining()];
                    buffer.get(array);//leggo i valori dal datagramma
                    sfidante = new String(array, StandardCharsets.UTF_8);


                        synchronized (lock1){
                            newchallenge=true;//metto il valore di newchallenge a true
                        System.out.println(sfidante+" wants to challenge answer yes or no");// propongo la sfida all'utente
                    }


                } catch (AsynchronousCloseException e){
                    //l'utente ha effettuato il logout
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
