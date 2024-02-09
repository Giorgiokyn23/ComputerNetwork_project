package org.Word_Quizzle.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ClientMainClass {
    public static void main(String[] args) throws RemoteException, NotBoundException {

        String serverAddress; //dichiaro il server addres del client
        if (args.length == 0) //se non ci sono argomenti utilizzo l'indirizzo di loopback locale
            serverAddress = "127.0.0.1"; //indirizzo locale
        else
            serverAddress = args[0]; //altrimenti utilizzo l'indirizzo che mi è stato passato come primo argomento

        WordQuizzleClient client = new WordQuizzleClient(serverAddress); //istanzio un nuovo WordQuizzleClient sull'indirizzo passatomi
        while (true) {
            client.elabora();// il client si mette in ascolto, in attesa di comandi da linea di comando

        }
    }
}
