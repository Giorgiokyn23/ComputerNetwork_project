package org.Word_Quizzle.Word_QuizzleServer;

public class ServerMainClass {
    public static void main(String[] args) {
        WordQuizzleServer server = new WordQuizzleServer();//istanzio un WorldQuizzleServer
        server.Listening_Started();//il Server si mette in ascolto
    }
}