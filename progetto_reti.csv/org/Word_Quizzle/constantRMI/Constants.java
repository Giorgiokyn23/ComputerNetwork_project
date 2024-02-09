package org.Word_Quizzle.constantRMI;

public final class Constants {
    public static final byte SUCCESS_CODE = 0;
    public static final byte BAD_FORMAT_CODE = 1;
    public static final byte WRONG_CREDENTIAL_CODE = 3;
    public static final byte INVALID_REQUEST_CODE = 5;
    public static final byte NON_EXISTENT_USER_CODE = 7;
    public static final byte FOREVER_ALONE_CODE = 11;
    public static final byte USER_UNAVAILABLE_CODE = 13;
    public static final byte NOT_FRIEND_CODE = 17;
    public static final byte CONNECTION_ERROR = 19;
    public static final byte NEW_WORD_CODE = 23;
    public static final byte RESULT_CODE = 29;
    public static final int WordNumber=7;

    public static final int RMI_SERVER_PORT = 1535;
    public static final int TCP_SERVER_PORT = 1536;



    //Comandi
    public static final String ComHelp = "--help";
    public static final String cmd_login = "login";
    public static final String cmd_registra_utente = "registra_utente";
    public static final String cmd_logout = "logout";
    public static final String cmd_aggiungi_amico = "aggiungi_amico";
    public static final String cmd_lista_amici = "lista_amici";
    public static final String cmd_mostra_punteggio = "mostra_punteggio";
    public static final String cmd_mostra_classifica = "mostra_classifica";
    public static final String cmd_sfida = "sfida";
    //Messaggi

    public static String Time_expired = "il tempo di risposta è terminato";
    public static String Error_code = "ERROR CODE";
    public static String help=("USAGE: COMMAND [ARGS...]\n"	+
            "Commands:\n" +
            "    - Registra l'utente\n" +
            "          registra_utente <nickUtente> <password>\n" +
            "    - Effettua il login\n" +
            "          login <nickUtente> <password>\n" +
            "    - Effettua il logout\n" +
            "          logout\n" +
            "    - Crea relazione di amicizia con nickUtente\n" +
            "          aggiungi_amico <nickUtente>\n" +
            "    - Mostra la lista dei propri amici\n" +
            "          lista_amici\n" +
            "    - Richiesta di una sfida a nickAmico\n" +
            "          sfida <nickUtente>\n" +
            "    - Mostra il punteggio dell'utente\n" +
            "          mostra_punteggio\n" +
            "    - Mostra una classifica degli amici dell'utente (incluso l'utente stesso)\n" +
            "          mostra_classifica");
}





