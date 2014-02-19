var rendererGlobalVar = (function() {
    var imageId; //shared variable available only inside your module
	var socket;
	var taskImage;

    return {
        setImageId: function(val) {
            imageId=val;
        },
        getImageId: function() {
            return imageId;
        },
		setSocket: function(val) {
			socket=val;
		},
		getSocket: function() {
            return socket;
        },
		setTaskImage: function(val) {
            taskImage=val;
        },
		getTaskImage: function(val) {
			return taskImage;
		}
    };
})();


(function(){

  // Polyfills
  window.requestAnimFrame = (function(){
  return window.requestAnimationFrame ||
          window.webkitRequestAnimationFrame ||
          window.mozRequestAnimationFrame ||
          window.oRequestAnimationFrame ||
          window.msRequestAnimationFrame ||
          function( callback ){
            window.setTimeout(callback, 1000 / 60);
          }; 
          })();

  var insideIframe = (window.parent !== window);
  var isMobile = /ipad|iphone|android/i.test(navigator.userAgent);

  // CONSTANTS
  var PEN = 'red';
  var ERASER = "rgba(255,255,255,1.0)";
  var TRACKER = 'red';
  var SIZE = 5;
  var MIN_SEND_RATE = 50; // the min interval in ms between 2 send
  
  // STATES
  var pid;
  var pname;
  var meCtx;
  var role;
  var tagging=false;
  
  // TOOLS STATUS
  var color = PEN;
  var size = SIZE;
  
  //GAME STATUS
  var score = 0;
  var defaultRoundTime=90;
  var matchStarted = false;
  var guessWord="";
  var guessed=false;
  var drawTool = true;
  var taskImage;
  
  var trackX;
  var trackY;

  var numTrace = 1;
  var onSocketMessage;
  var dirtyPositions = false;

  var viewport = document.getElementById('viewport');

  
  // WebSocket
  var connected = false;


  function connect () {
    try {
	  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
	  socket = new WS($('#renderWebSocket').data('ws'));
	  
      socket.onmessage = onSocketMessage;

      socket.onopen = function(evt) {
		console.log( evt, this );
        connected = true;
		rendererGlobalVar.setSocket(socket);
      };

      socket.onclose = function (evt) {
            connected = false;
      };

      socket.onerror = function(evt) {console.error("error", evt);};
	  
    }
    catch (e) {
		console.error("Error in the connection");
		console.error(e);
    }
  }

  function send (o) {
    if (!connected) return;
    socket.send(JSON.stringify(o));
  }

//***************************TAKING CARE OF THE LINE STROKES*****************************/
var gameloop = (function(){
  var canvas = document.getElementById("draws");
  var ctx = canvas.getContext("2d");
  var taskCanvas = document.getElementById("task");
  var taskContext = taskCanvas.getContext("2d");
  var maskCanvas = document.getElementById("mask");
  var maskContext = maskCanvas.getContext("2d");
  taskContext.font="30px Arial";
  taskContext.fillText("Loading the list of tags...",10,50);
  /*******************************MANAGING THE INCOMING MESSAGES*****************/
  onSocketMessage = function (e) {
    var m = JSON.parse(e.data);
	switch(m.type)
	{
			
		case "task"://Send the current task (image + tag) to the player
                            tagging=false;
                            guessWord=m.word;
                            taskImage=null;
                            taskImage=new Image();
                            taskImage.src=m.image;
							taskContext.font="30px Arial";
							taskContext.fillText("Loading Image...",10,50);
							maskContext.font="30px Arial";
							maskContext.fillText("Select an available tag from",10,50);
							maskContext.fillText("the dropdown menu.",10,80);
							rendererGlobalVar.setImageId(m.id);
                            //Wait for the image to be loaded before drawing it to canvas to avoid
                            //errors
                            taskImage.onload = function() {
                                            taskContext.save();
                                            taskContext.beginPath();
                                            x=0;
                                            y=0;
											taskCanvas.width=m.width;
											taskCanvas.height=m.height;
											canvas.width=m.width;
											canvas.height=m.height;
                                            taskContext.drawImage(taskImage,0,0,m.width,m.height);
                                            taskContext.restore();
                            };
							rendererGlobalVar.setTaskImage(taskImage);
 			break;
		
		case "trace":
							ctx.lineJoin = "round";
							
                            ctx.beginPath();
                                    var points = m.points;
									var drawable=true;
                                    ctx.moveTo(points[0].x, points[0].y);
									for(var i=0;i<points.length;i++)
									{
										var p=points[i];
										if(p.removed===false&&p.color!='end')
										{
												if(!drawable)
												{
													drawable=true;
													ctx.beginPath();
													ctx.moveTo(p.x,p.y);
												}
												ctx.strokeStyle=m.color;
												ctx.lineWidth = p.size;
												ctx.lineTo(p.x, p.y);
										}
										else
										{
											if(drawable)
											{
												ctx.stroke();
												drawable=false;
											}
										}
										
                                    };								
			break;
		
		case "tags":
							var tagList=m.tags;
							var html = "<option></option>";
							for (i=0;i<tagList.length;i++) {
								html+="<option>"+tagList[i]+"</option>";
							}
							$("#tagList").html(html);	
							$("#tagList").attr("disabled",false);
							$("#tagForm").show();
			
			break;
	}
	dirtyPositions = true;
  };

  var w = canvas.width, h = canvas.height;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
})();

  connect();
})();

function tagSelected() { 
	var canvas = document.getElementById("draws");
	var ctx = canvas.getContext("2d");
	var taskCanvas = document.getElementById("task");
	var taskContext = taskCanvas.getContext("2d");
	var maskCanvas = document.getElementById("mask");
	var maskContext = maskCanvas.getContext("2d");
	maskCanvas.width=taskCanvas.width;
	maskCanvas.height=taskCanvas.height;
	var tag = $("#tagList option:selected").text();
	if(tag!="") {
		maskContext.font="30px Arial";
		maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
		ctx.clearRect(0,0,canvas.width,canvas.height);
		maskContext.fillText("Computing aggregated mask...",10,50);
		var serverSocket = rendererGlobalVar.getSocket();
		serverSocket.send(JSON.stringify({"type":"tag","tag":tag}));
		maskImage = null;
		maskImage = new Image();
		maskImage.src = "/retrieveMask?imageID="+rendererGlobalVar.getImageId()+"&tag="+tag;
		maskImage.onload = function() {
			maskContext.save();
			maskContext.beginPath();
			x=0;
			y=0;
			maskCanvas.width=taskCanvas.width;
			maskCanvas.height=taskCanvas.height;
			maskContext.drawImage(rendererGlobalVar.getTaskImage(),0,0,maskCanvas.width,maskCanvas.height);
			maskContext.drawImage(maskImage,0,0,maskCanvas.width,maskCanvas.height);
			maskContext.restore();
		};
	}
	//If they chose the white tag, just clean the canvas
	else {
		ctx.clearRect(0,0,canvas.width,canvas.height);
		maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
		maskContext.font="30px Arial";
		maskContext.fillText("Select an available tag from",10,50);
		maskContext.fillText("the dropdown menu.",10,80);
	}
};