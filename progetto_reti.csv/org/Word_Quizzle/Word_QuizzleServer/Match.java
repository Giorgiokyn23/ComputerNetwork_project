package org.Word_Quizzle.Word_QuizzleServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.Word_Quizzle.constantRMI.Constants;

import java.io.*;
import java.net.URL;
import java.util.*;

public class Match {

    private static ObjectMapper converter = new ObjectMapper();
    private static BufferedReader reader;
    private static List<String> possibleWords;

    static {
        try {
            reader = new BufferedReader(new FileReader(new File("italianWORDS.txt"))); //a tempo di loading leggo le 900 parole circa da file
            possibleWords = new ArrayList();//inizializzo un arraylist in cui mettere le parole
            while(reader.ready())//fino a che ci sono parole
                possibleWords.add(reader.readLine().trim());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String challenger;
    private String challenged;
    private boolean challengedHasLeft;
    private boolean challengerHasLeft;
    private int challengerScore;
    private int challengedScore;
    private String winner;

    Thread translationThread;
    List<String> translationList;           //traduzioni corrette
    Queue<String> challengerList;           //parole rimaste da tradurre
    Queue<String> challengerGuessList;      //traduzioni inviate
    Queue<String> challengedList;           //parole rimaste da tradurre
    Queue<String> challengedGuessList;      //traduzioni inviate

    public Match(String challenger, String challenged) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.challengerHasLeft = false;
        this.challengedHasLeft = false;
        this.challengerScore = 0;
        this.challengedScore = 0;
        this.winner = null;

        List <String> currentWordList = selectRandomWords();

        translate(currentWordList); //eseguito in un thread parallelo

        challengerList = new LinkedList<>(currentWordList);
        challengerGuessList = new LinkedList<>();
        challengedList = new LinkedList<>(currentWordList);
        challengedGuessList = new LinkedList<>();
    }

    private void translate(List<String> currentWordList) {
        this.translationThread = new Thread(()->{
            this.translationList = currentWordList.stream().
                    map(Match::Obtain_Translation).collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
        });
        translationThread.start();
    }

    private static String Obtain_Translation(String word){
        try {
            URL url = new URL("https://api.mymemory.translated.net/get?q="+word+"&langpair=it|en");
            return converter.readValue(url, ObjectNode.class).get("responseData").get("translatedText").textValue();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> selectRandomWords() {//lista creata in cui seleziono  le traduzioni
        ArrayList<String> list = new ArrayList<>(Constants.WordNumber);
        for (int i = 0; i < Constants.WordNumber; i++) {
            String word = possibleWords.get((int) (Math.random()*Match.possibleWords.size()));
            list.add(word);
        }
        return list;
    }

    public String nextWord(String nickname) {//mando la parola successiva facendo un pool sulla lista rimuovendo la parola in testa

        if(nickname.equals(challenger))
            return challengerList.poll();
        else
            return challengedList.poll();
    }

    public void putGuess(String nickname, String guess) {

        if(nickname.equals(challenger)) {//riempio la lista di parole tradotte dall'utente(nickname)
            challengerGuessList.add(guess);//riempio quella dello sfidante
        } else {
            challengedGuessList.add(guess);//riempio quella dello sfidato
        }
    }

    public String getOpponent(String nickname) {//utilizzata nella GUESSWORD
        if(nickname.equals(challenger))
            return this.challenged;
        else
            return this.challenger;
    }

    public void playerTimeout(String nickname) {
        if(nickname.equals(challenger))
            this.challengerList.clear();
        else
            this.challengedList.clear();
    }

    public boolean hasPlayerEnded(String nickname) {//utilizzata per Controllare nella GUESSWORD se il l'avversario ha concluso,in caso endmatch
        if(nickname.equals(challenger))
            return this.challengerList.isEmpty();
        else
            return this.challengedList.isEmpty();
    }

    public void playerLeaves(String nickname) {//utilizzata per controllare se un player
        if(nickname.equals(challenger)) {
            this.challengerHasLeft = true;
            this.challengerList.clear();
        } else {
            this.challengedHasLeft = true;
            this.challengedList.clear();
        }
    }

    public boolean hasPlayerLeft(String nickname) {// se un giocatore lascai la partita do -10 di penalità,utilizzata in WordQUizzleServer
        if(nickname.equals(challenger))
            return this.challengerHasLeft;
        else
            return this.challengedHasLeft;
    }

    public void computeResult() {//funzione che computa i risultati
        try {
            this.translationThread.join();
            Iterator<String> correct;

            correct = this.translationList.iterator();
            for (String guess : challengerGuessList) {
                if(areTheSame(guess, correct.next()))
                    challengerScore += 3;
                else
                    challengerScore -= 1;
            }

            correct = this.translationList.iterator();
            for (String guess : challengedGuessList) {
                if(areTheSame(guess, correct.next()))
                    challengedScore += 3;
                else
                    challengedScore -= 1;
            }


            if(challengerHasLeft)
                this.challengerScore = -10;
            if(challengedHasLeft)
                this.challengerScore = -10;

            if(challengerScore > challengedScore) {
                challengerScore += 3;
                this.winner = challenger;
            } else if(challengedScore > challengerScore) {
                challengedScore += 3;
                this.winner = challenged;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean areTheSame(String a, String b) {//funzione utilizzzata per il confronto tra traduzioni
        String aSimplified = a.trim().toLowerCase();
        String bSimplified = b.trim().toLowerCase();
        return aSimplified.equals(bSimplified);
    }

    public String getResult(String nickname) {
        if(nickname.equals(challenger)){
            if(challenger.equals(winner))
                return "You won! You scored "+challengerScore+" points, your opponent "+challengedScore;
            else if(challenged.equals(winner))
                return "You lost!  You scored "+challengerScore+" points, your opponent "+challengedScore;
            else
                return "it's a draw! You scored "+challengerScore+" points, your opponent "+challengedScore;
        }
        else{
            if(challenged.equals(winner))
                return "You won! You scored "+challengedScore+" points, your opponent "+challengerScore;
            else if(challenger.equals(winner))
                return "You lost!  You scored "+challengedScore+" points, your opponent "+challengerScore;
            else
                return "it's a draw! You scored "+challengedScore+" points, your opponent "+challengerScore;
        }
    }

    public int getScore(String nickname) {//funzione che ritorna lo score del nickname
        if(nickname.equals(challenger))
            return this.challengerScore;
        else
            return this.challengedScore;
    }
}
