package org.Word_Quizzle.client;

import org.Word_Quizzle.constantRMI.Constants;
import org.Word_Quizzle.constantRMI.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NioTools {
    public static Response Receive_Response(SocketChannel client){
        try {

            ByteBuffer[] responseHeader = new ByteBuffer[2];// alloco un byte buffer
            responseHeader[0] = ByteBuffer.allocate(1); // la prima parte sarà l'header da 1 byte
            responseHeader[1] = ByteBuffer.allocate(Integer.BYTES); //la seconda parte sarà il corpo da 4 byte
            client.read(responseHeader); // leggo dal Socketchannel associato al client la sequenza di byte
            byte responseCode = responseHeader[0].flip().get(); //eseguo una flip e faccio una get per leggere i dati
            int responseLength = responseHeader[1].flip().getInt();//guardo la lunghezza del corpo che ricordiamo è a lunghezza variabile
            ByteBuffer responseData = ByteBuffer.allocate(responseLength);//alloco un bytebuffer di capacità responseLength
            client.read(responseData);//leggo dal Socket channel la risposta
            byte[] array = new byte[responseData.flip().remaining()];//alloco un byte array dalla posizione corrente alla fine per leggere il messaggio
            responseData.get(array);//leggo il messaggio
            String message = new String(array, StandardCharsets.UTF_8);//costruisco la stringa in UTF_8
            System.out.println("Ho ricevuto: "+message);//printo a schermo il messaggio
            return new Response(responseCode, message);//ritorno la risposta
        } catch (IOException e) {
            e.printStackTrace();
            return new Response(Constants.CONNECTION_ERROR, "");
        }
    }

    public static void sendRequest(SocketChannel client, String ... args){
        if(checkLength(args)){ //controllo che gli argomenti passati non siano troppo lunghi
            try {
                StringBuilder request = new StringBuilder(); // costruisco uno string builder con apacità iniziale 16 caratteri di default
                for(String s : args)
                    request.append(s).append(" ");//costruisco la richiesta da mandare
                request.append("\n");//termino con \n
                System.out.println("mando: "+request.toString().trim()); //stampo a schermo la richiesta
                client.write(ByteBuffer.wrap(request.toString().getBytes(StandardCharsets.UTF_8)));//codifico la stringa in una sequenza di bytes e la scrivo sulla socket
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean checkLength(String... args){
        boolean correctLength = Arrays.stream(args).allMatch(s -> s.length()>0 && s.length()<=60);//controlla che il numero dei caratteri sia minore di 60
        if(!correctLength)
            System.out.println("campi digitati troppo lunghi");
        return  correctLength;
    }

}
