var rendererGlobalVar = (function() {
	var socket;


    return {
		setSocket: function(val) {
			socket=val;
		},
		getSocket: function() {
            return socket;
        }
    };
})();


function loadMask(tag) {

var selectionimg = $("#ImgAttivattiva").val();
var mediaimg = $("#mediaLocator").val();
var canvas = document.getElementById("draws")
    ,ctx = canvas.getContext("2d")
    ,taskCanvas = document.getElementById("task")
    ,taskContext = taskCanvas.getContext("2d");
var taskImage=new Image();
taskImage.src=mediaimg;

 var graph1 = document.getElementById("chart_div1");
 var graph2 = document.getElementById("chart_div2");
 var canvas = document.getElementById("viewport");

 $(canvas).show();
 $(graph1).hide();
 $(graph2).hide();



$("#ImgPreview").val($("#ImgAttivattiva").val());

 taskImage.onload = function() {
             taskContext.save();
             taskContext.beginPath();
             taskCanvas.width=this.width;
             taskCanvas.height=this.height;
             canvas.width=this.width;
             canvas.height=this.height;
             taskContext.drawImage(taskImage,0,0,this.width,this.height);
             taskContext.restore();
             //Clear the mask canvas
             var maskCanvas = document.getElementById("mask");
             var maskContext = maskCanvas.getContext("2d");
             maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
         };


try {
	  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
	  //socket = new WS("ws://localhost:9000/rendererStream?imageID="+selectionimg);
	  socket = new WS("ws://www.sketchness.com/rendererStream?imageID="+selectionimg);

      socket.onmessage = onSocketMessage;

      socket.onopen = function(evt) {
		console.log( evt, this );
        connected = true;
        rendererGlobalVar.setSocket(socket);
		drawTracesMask(taskImage,selectionimg,tag);
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

var drawTracesMask = function(taskImage,selectionimg,tag) {
	var canvas = document.getElementById("draws");
	var ctx = canvas.getContext("2d");
	var taskCanvas = document.getElementById("task");
	var taskContext = taskCanvas.getContext("2d");
	var maskCanvas = document.getElementById("mask");
	var maskContext = maskCanvas.getContext("2d");
	maskCanvas.width=taskCanvas.width;
	maskCanvas.height=taskCanvas.height;

		maskContext.font="30px Arial";
		maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
		ctx.clearRect(0,0,canvas.width,canvas.height);
		maskContext.fillText("Computing aggregated mask...",10,50);

        var serverSocket = rendererGlobalVar.getSocket();
        serverSocket.send(JSON.stringify({"type":"tag","tag":tag}));
        var maskImage = null;
        maskImage = new Image();
        maskImage.src = "/retrieveMask?imageID="+selectionimg+"&tag="+tag;


        maskImage.onload = function() {

        if((maskImage.src.substring(maskImage.src.indexOf("=")+1,maskImage.src.indexOf("&"))==$("#ImgAttivattiva").val())&& ($("#ImgPreview").val() == $("#ImgAttivattiva").val())){
            maskContext.save();
            maskContext.beginPath();

            maskCanvas.width=taskCanvas.width;
            maskCanvas.height=taskCanvas.height;
            maskContext.drawImage(taskImage,0,0,maskCanvas.width,maskCanvas.height);
            maskContext.drawImage(maskImage,0,0,maskCanvas.width,maskCanvas.height);
            maskContext.restore();
            };
        }
}


function send (o) {
    if (!connected) return;
    socket.send(JSON.stringify(o));
  }


var gameloop = (function(){
  var canvas = document.getElementById("draws");
  var ctx = canvas.getContext("2d");
  var taskCanvas = document.getElementById("task");
  var taskContext = taskCanvas.getContext("2d");
  var maskCanvas = document.getElementById("mask");
  var maskContext = maskCanvas.getContext("2d");

  /*******************************MANAGING THE INCOMING MESSAGES*****************/
  onSocketMessage = function (e) {
    var m = JSON.parse(e.data);



    switch(m.type)
    	{


    		case "trace":
    		                if((m.imageId == $("#ImgAttivattiva").val())&& (m.imageId == $("#ImgPreview").val())){
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
                            }
    			break;


    	}


    };

    var w = canvas.width, h = canvas.height;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
  })();