package org.Word_Quizzle.constantRMI;

public class Response {
    public byte code;
    public String message;

    public Response(byte code, String message) {
        this.code = code;
        this.message = message;
    }
}
