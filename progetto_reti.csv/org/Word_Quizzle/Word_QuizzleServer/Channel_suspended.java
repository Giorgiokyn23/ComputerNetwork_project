package org.Word_Quizzle.Word_QuizzleServer;

import org.Word_Quizzle.userTools.User_Session;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Channel_suspended { // un channel suspended ha un SocketChannel collegato all'utente
    //una User_Session nel package userTools
    SocketChannel channel;
    User_Session session;
    String username;

    public Channel_suspended(SelectionKey key){
        this.channel = (SocketChannel) key.channel();
        this.session = (User_Session) key.attachment();
        this.username = session.nickname;
    }
}
