package org.Word_Quizzle.Word_QuizzleServer;

import org.Word_Quizzle.userTools.User;
import org.Word_Quizzle.userTools.User_Manager;
import org.Word_Quizzle.constantRMI.Registration_Service;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class Registration_Server extends RemoteObject implements Registration_Service {
    User_Manager manager;

    protected Registration_Server(User_Manager manager) throws RemoteException {

            this.manager = manager;

    }

    @Override
    public boolean registerUser(String nickname, String password) throws RemoteException {//funzione per la registrazione tramite RMI
        System.out.println("RICHIESTA ISCRIZIONE");
        if(!manager.addUser(new User(nickname, password)))
            return false;
        System.out.println("iscrizione eseguita");
        return true;
    }
}
