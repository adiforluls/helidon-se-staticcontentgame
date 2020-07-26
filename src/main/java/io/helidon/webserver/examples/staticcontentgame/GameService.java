
package io.helidon.webserver.examples.staticcontentgame;

import java.util.Collections;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import java.lang.Math;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/*
* Get winner's name (GET): curl -X GET http://localhost:8080/game/winner   
* Create a new user (POST): curl -d '{"username" : "qwerty"}' -H 'Content-Type: application/json' http://localhost:8080/game/user
* Enter the registered user's guess (PUT): 
* curl -H 'Content-Type: application/json' -X PUT -d '{"username" : "qwerty", "guess" :"10"}' http://localhost:8080/game
*/

public class GameService implements Service {

    private static final Logger LOGGER = Logger.getLogger(GameService.class.getName());
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    HashMap<String,Integer> UsedNames = new HashMap<>();
    HashMap<String,RegUser> data = new HashMap<>();

    private static int sum = 0;       // to store overall sum of the user's responses
    private static int activeusers = 0;   // to track users who have guessed atleast once
    private static int attempts = 0;       // Useful in tie-breaker

    private static String Winner;

    GameService(Config config){
        Winner = config.get("app.winner").asString().orElse("lol");
    }

    @Override
    public void update(Routing.Rules rules){

        rules.get("/winner", this::getWinner)
             .post("/user", this::HandleNewUser)
             .put("/", this::HandleUserGuess);
    }

    //(GET) API to display the winner's username
    private void getWinner(ServerRequest request, ServerResponse response)
    {
        JsonObject jsonobject;

        if(activeusers == 0)
        {
            jsonobject = JSON.createObjectBuilder()
                .add("Winner", "The game hasn't started yet!")
                .build();
        }

        else{
            jsonobject = JSON.createObjectBuilder()
                .add("Winner", Winner + " is the current winner!")
                .build();
        }
        response.status(Http.Status.ACCEPTED_202).send(jsonobject);
    }


    // (POST) API for handling a new user
    private void NewUser(JsonObject jsonobject, ServerResponse response)
    {
        // throw error if keys are missing
        if(!jsonobject.containsKey("username"))
        {
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "Username missing")
                    .build();
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;
        }

        String username = jsonobject.getString("username").toString();

        // throw error if username is already taken
        if(UsedNames.containsKey(username) || username.isEmpty())
        {
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "Sorry! This Username is taken or you didn't enter one")
                    .build();
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;
        }

        RegUser gamer = new RegUser();

        gamer.setUserName(username);

        UsedNames.put(username, 1);
        data.put(username, gamer);

        JsonObject entity = JSON.createObjectBuilder()
                        .add("success", "Your response was successfully recorded.")
                        .build();
        response.status(Http.Status.ACCEPTED_202).send(entity);
    }


    // (PUT) API for registered user's response 
    private void UserGuess(JsonObject jsonobject, ServerResponse response)
    {
        // throw error if required keys are missing
        if(!jsonobject.containsKey("username"))
        {
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "username missing")
                    .build();
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;
        }

        if(!jsonobject.containsKey("guess"))
        {
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "guess missing")
                    .build();
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;
        }


        // retrieve the username and numerical guess
        String username = jsonobject.getString("username").toString();
        int guess;
        
        try{
            guess = Integer.parseInt(jsonobject.getString("guess").toString());
        }
        catch(NumberFormatException e){
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "Enter a numerical guess.")
                    .build();
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;
        }

        // throw error if username is not registered
        if(!UsedNames.containsKey(username))
        {
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "Username does not exist!")
                    .build();
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;
        }

        if(guess>100 || guess<0)
        {
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", "Guess is out of bounds. Please enter an integer between 0-100!")
                    .build();
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;   
        }

        RegUser gamer = data.get(username);

        data.remove(username);

        // check if it's the first time the user is guessing
        if(gamer.getGuess() == -1)
        {
            // increment the number of active users
            activeusers+=1;
            sum+=guess;
        }
        else sum+=(guess - gamer.getGuess());

        //increase the number of overall attempts
        attempts+=1;

        // change the user's guess
        gamer.setGuess(guess);
        gamer.setAttempt(attempts);

        data.put(username, gamer);

        // check if we have a new winner
        updateWinner();

        JsonObject entity = JSON.createObjectBuilder()
                        .add("success", "Your response was successfully recorded.")
                        .build();
        response.status(Http.Status.ACCEPTED_202).send(entity);
    }

    // check for any update in the winner after every user's guess
    private void updateWinner()
    {
        double mindifference = 105, twoThirdAvg = (2.0*((double)sum))/(3.0*((double)activeusers));
        int minIndex = attempts;
        
        for(RegUser gamer : data.values())
        {
            // Only consider the registered users who have responded
            if(gamer.getGuess()!=-1)
            {
                if(mindifference > Math.abs(twoThirdAvg - (double)(gamer.getGuess())))
                {
                    // update winner
                    mindifference = Math.abs(twoThirdAvg - (double)(gamer.getGuess()));
                }
            }
        }

        // Winner will be the one with the earliest attempt out of all the valid contenders
        for(RegUser gamer : data.values())
        {
            if(gamer.getGuess()!=-1)
            {
                if(mindifference == Math.abs(twoThirdAvg - (double)(gamer.getGuess())))
                {
                    if(minIndex>=gamer.getAttempt())
                    {
                        Winner = gamer.getUserName();
                        minIndex = gamer.getAttempt();
                    }
                }
            }
        }
    }

    // To handle errors caused by the server
    private static Void sendError(Throwable throwable, ServerResponse res) {

        JsonObject jsonobject = JSON.createObjectBuilder()
            .add("error", "Something's went wrong")
            .build();

        res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        res.send(jsonobject);
        return null;

    }

    // To pass request as json object to the api
    private void HandleNewUser(ServerRequest request, ServerResponse response) {
        request.content().as(JsonObject.class)
            .thenAccept(jsonobject -> NewUser(jsonobject, response))
            .exceptionally(e -> sendError(e,response));
    }

    // To pass request as json object to the api
    private void HandleUserGuess(ServerRequest request, ServerResponse response) {
        request.content().as(JsonObject.class)
            .thenAccept(jsonobject -> UserGuess(jsonobject, response))
            .exceptionally(e -> sendError(e,response));
    }

}
