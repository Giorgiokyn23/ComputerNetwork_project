package org.Word_Quizzle.userTools;

public class NoUserException extends Throwable {
    String user;

    public NoUserException(String user) {
        this.user = user;
    }

}
