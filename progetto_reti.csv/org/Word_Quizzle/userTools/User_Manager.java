package org.Word_Quizzle.userTools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.stream.Stream;

public class User_Manager extends HashMap<String, User> {

    static ObjectMapper converter = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);//utilizzato per la (de)serializzazione, e per l'elaborazione delle risposte


    public User_Manager(){
        super(50);
    }

    public synchronized boolean containsUser(String nickname) {
        return this.containsKey(nickname);
    }

    public synchronized User getUser(String nickname) {
        return this.get(nickname);
    }

    public synchronized boolean addUser(User u) {
        if(this.containsKey(u.nickname))
            return false;
        else {
            this.put(u.nickname, u);
            return true;
        }
    }

    public synchronized boolean checkPassword(String nickname, String password) {
        return this.get(nickname).Right_Password(password);
    }

    public synchronized void addFriendship(String a, String b) throws NoUserException {
        if(!this.containsKey(a))
            throw new NoUserException(a);
        else if (!this.containsKey(b))
            throw new NoUserException(b);
        else{
            //User.friends é un set, quindi non aggiunge stringhe uguali
            this.get(a).friends.add(b);
            this.get(b).friends.add(a);
        }
    }

    public synchronized String showScore(String user){
        try {
            return converter.writeValueAsString(this.get(user).scoreInJson());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "errore interno al server";
        }
    }

    public synchronized String showFriend(String user) throws NoUserException {
        try{
            if(!this.containsKey(user)) {
                throw new NoUserException(user);
            }
            else
                return converter.writeValueAsString(this.get(user).friends);
        }
        catch (JsonProcessingException e){
            e.printStackTrace();
            return "errore interno al server";
        }
    }

    public synchronized String showRanking(String user) throws NoUserException {
        if(!this.containsKey(user))
            throw new NoUserException(user);
        else{

            return Stream.concat(this.get(user).friends.stream(), Stream.of(user))//prendo tutti gli amici, aggiungo user, li ordino per punteggio decrescente
                    //(utilizzando un comparator compare(a, b){return b.score - a.score})
                    //di ognuno creo un breve jsonObject {user:"", score:x}
                    //raccolgo tutti i jsonObject dentro un JsonArray
                    .map(userName -> this.get(userName))
                    .sorted((a, b) -> b.score-a.score)
                    .map(User::scoreInJson)
                    .collect(JsonNodeFactory.instance::arrayNode, ArrayNode::add, ArrayNode::addAll)
                    .toPrettyString();
        }
    }

    public synchronized boolean isFriend(String user, String friend) {
        return this.get(user).friends.contains(friend);
    }

    public synchronized boolean isOnline(String user) {
        return this.get(user).socketAddress != null;
    }

    public synchronized void setUserOnline(String user, InetSocketAddress address) {
        this.get(user).setSocketAddress(address);
    }

    public synchronized void set_offline_user(String user){
        this.get(user).unsetSocketAddress();
    }

    public synchronized void updateUserScore(String user, int n){
        this.get(user).score += n;
    }

    //seralization methods
    public synchronized void storeInFile(File path) throws IOException {
        converter.writeValue(path, this);
    }

    public synchronized String writeString() throws JsonProcessingException {
        return converter.writeValueAsString(this);
    }

    //deserialization methods
    public synchronized  static User_Manager  parseFromFile(File path) throws IOException {
        return converter.readValue(new FileInputStream(path), User_Manager.class);
    }

    public synchronized static User_Manager parseFromString(String data) throws JsonProcessingException {
        return converter.readValue(data, User_Manager.class);
    }
}
