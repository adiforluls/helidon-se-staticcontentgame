function userRegister(){

    var usr = document.getElementById("newusername").value;

    var xhr = new XMLHttpRequest();

    xhr.open("POST", "/game/user", true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onload = function(){

        var msg = JSON.parse(xhr.responseText);

        if(xhr.status == 202){
            /*iframe.contentDocument.body.innerHTML=msg["success"];*/
            document.getElementById("status1").innerHTML=msg["success"];
        }
        else {
            /*iframe.contentDocument.body.innerHTML=msg["error"];*/
            document.getElementById("status1").innerHTML=msg["error"];
        }
    }

  /*iframe.onload = function()
    {
        console.log(iframe.contentDocument.body.innerHTML);
    }*/

     xhr.send(JSON.stringify({username: usr}));
}

function userGuess(){

    var usr = document.getElementById("regusername").value;
    var gus =  document.getElementById("guess").value;

    var xhr = new XMLHttpRequest();

    xhr.open("PUT", "/game", true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onload = function(){

        var msg = JSON.parse(xhr.responseText);

        if(xhr.status == 202){
          //  iframe.contentDocument.body.innerHTML=msg["success"];
          document.getElementById("status2").innerHTML=msg["success"];
        }
        else {
           // iframe.contentDocument.body.innerHTML=msg["error"];
           document.getElementById("status2").innerHTML=msg["error"];
        }
    }
   /* iframe.onload = function()
    {
        console.log(iframe.contentDocument.body.innerHTML);
    }
    */
    xhr.send(JSON.stringify({username: usr, guess: gus}));
}

function getWinner(){

    var xhr =  new XMLHttpRequest();
    xhr.open("GET", "/game/winner", true);

    xhr.onload = function(){

        var msg = JSON.parse(xhr.responseText);
        
        if(xhr.status == 202){
            document.getElementById("win").innerHTML = msg["Winner"];
        }

    }

    xhr.send();
}

function myFunction() {
    alert("We are from the future and here to play a game" +  
    " choose an integer between 0 to 100 that you think" + 
    " will be the closest to 2/3 of the average of the input from all the users." +
    " To play, head over to New User section and register."
    );
  }