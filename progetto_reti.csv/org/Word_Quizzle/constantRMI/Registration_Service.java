package org.Word_Quizzle.constantRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Registration_Service extends Remote {

     boolean registerUser(String nickname, String password) throws RemoteException;

}
