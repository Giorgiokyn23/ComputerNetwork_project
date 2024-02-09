package org.Word_Quizzle.userTools;

import org.Word_Quizzle.Word_QuizzleServer.Match;
import org.Word_Quizzle.constantRMI.Response;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class User_Session { // classe utilizza ta per sospendere la sessione
    public String nickname = null;
    public Match currentMatch;
    public InetSocketAddress address;

    public ByteBuffer requestBuffer;
    public String responseMessage;
    public ByteBuffer[] responseBuffer = new ByteBuffer[3];

    public User_Session(InetSocketAddress address) {
        this.address = address;
        this.requestBuffer = ByteBuffer.allocate(256);
        this.responseBuffer[0] = ByteBuffer.allocate(1);
        this.responseBuffer[1] = ByteBuffer.allocate(Integer.BYTES);
    }

    public void putResponse(byte code, String message){// utilizzata per mandare il Response,
        this.responseMessage = message;
        Response response = new Response(code, message);
        responseBuffer[0].clear().put(response.code).flip();
        responseBuffer[1].clear().putInt(response.message.getBytes(StandardCharsets.UTF_8).length).flip();
        responseBuffer[2] = ByteBuffer.wrap(response.message.getBytes(StandardCharsets.UTF_8));
    }

    public void setCurrentMatch(Match currentMatch) {
        this.currentMatch = currentMatch;
    }

    public void unsetCurrentMatch() {
        this.currentMatch = null;
    }

    public void delete() {
        if(this.currentMatch != null){
            this.currentMatch.playerLeaves(this.nickname);
        }
    }
}
