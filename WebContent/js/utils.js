function login() {
    var data = new FormData(); // das ist unser Daten-Objekt ...
    data.append('username',document.getElementById('username').value)
    data.append('password',document.getElementById('password').value)
    $.ajax({
       url: 'rest/service/login',
       data: data,          // Das ist unser Datenobjekt.
       type: 'POST',         // HTTP-Methode, hier: POST
       processData: false,
       contentType: false,
       // und wenn alles erfolgreich verlaufen ist, schreibe eine Meldung
       // in das Response-Div
       success: function(res) { 
           console.log("Res: "+res)
    	   if(res==""){
    		   $('#loginspan').html("You are not logged in <button onClick=\"openLoginDialog()\">Login</button>")
    		   alert("Login failed!")
    	   }else{
        	 Cookies.set('bkguplifttoken',res)
        	 $('#loginspan').html("You are logged in <button onClick=\"logout()\">Logout</button>")
        	 $( "#logindialog" ).dialog("close")
    	   }
       }
    });
 }

function openLoginDialog(collid){
	$( "#logindialog" ).dialog( "open" );
}

function logout(){
	Cookies.remove('bkgtoken');
	$('#loginspan').html("You are not logged in <button onClick=\"openLoginDialog()\">Login</button>")
}

var logindialog = $( "#logindialog" ).dialog({
    autoOpen: false,
    height: 350,
    width: 500,
    modal: true,
    buttons: {
      "Login": login,
      Cancel: function() {
        logindialog.dialog( "close" );
      }
    },
    close: function() {
      form[ 0 ].reset();
      //allFields.removeClass( "ui-state-error" );
    }
  });

  form = logindialog.find( "form" ).on( "submit", function( event ) {
    event.preventDefault();
    login();
  });  
  if(Cookies.get('bkguplifttoken')){
	 $('#loginspan').html("You are logged in <button onClick=\"logout()\">Logout</button>")
  }else{
 	 $('#loginspan').html("You are not logged in <button onClick=\"openLoginDialog()\">Login</button>")
  }