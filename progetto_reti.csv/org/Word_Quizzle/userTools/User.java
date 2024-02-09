package org.Word_Quizzle.userTools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.TreeSet;

public class User implements Comparable<User> {


    //proprieties
    protected String nickname;
    protected String password;
    protected int score;
    protected TreeSet<String> friends;

    @JsonIgnore     //non serializzato
    public InetSocketAddress socketAddress = null;

    //constructors
    public User(){} // costruttori
    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
        this.score = 0;
        this.friends = new TreeSet<String>();
    }

    protected ObjectNode scoreInJson() { //utilizzata da SHOWSCORE per mostrare i punteggi

        return JsonNodeFactory.instance.objectNode()
                .put("nickname", this.nickname)
                .put("score", this.score);
    }

    protected boolean Right_Password(String s) {

        return (s.equals(this.password));
    }


    public void setSocketAddress(InetSocketAddress address) {
        this.socketAddress = address;
    }

    public void unsetSocketAddress() {
        this.socketAddress = null;
    }


    //confronti (coerenti) utili per l'hashmap
    @Override
    public int compareTo(User o) {
        return this.nickname.compareTo(o.nickname);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(nickname, ((User) o).nickname);
    }

    //getters and setters, per la serializzazione jackson
    public String getNickname() {
        return nickname;
    }

    public void set_Nickname(String nickname) {
        this.nickname = nickname;
    }

    public String get_Password() {
        return password;
    }

    public void set_Password(String password) {
        this.password = password;
    }

    public int get_Score() {
        return score;
    }

    public void set_Score(int score) {
        this.score = score;
    }

    public TreeSet<String> getFriends() {
        return friends;
    }

    public void set_Friends(TreeSet<String> friends) {
        this.friends = friends;
    }
}

