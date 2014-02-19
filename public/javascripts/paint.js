 require(["Communicator", "Time", "jquery", "i18n"], function(Communicator, Time, $) {

	$(function() {

		var time = new Time();

		var isIframe = (window.parent !== window);
		var isMobile = /ipad|iphone|android/i.test(navigator.userAgent);

		// CONSTANTS
		var CONSTANTS = {
			PEN_COLOR: 'red',
			PEN_SIZE: 5,
			ERASER_COLOR: "rgba(255,255,255,1.0)",
			ERASER_SIZE: 25,
			TRACKER_COLOR: 'red',
			MIN_SEND_RATE: 50, // the min interval in ms between 2 send
			DEFAULT_ROUND_TIME: 120,
			DEFAULT_ASK_TIME: 30
		};

		// STATES
		var user = {
			id: null,
			name: null,
		};

		// TOOLS STATUS
		var tool = {
			color: CONSTANTS.PEN_COLOR,
			size: CONSTANTS.PEN_SIZE,
			draw: true
		};

		//GAME STATUS
		var game = {
			role: null,
			score: 0,
			matchStarted: false,
			guessWord: "",
			guessed: false,
			taskImage: null,
			tagging: false,
			traceNum: 1
		};

		// every player positions
		var players = [];

		// Dunno, variables before in an enclosure causing problems
		var skipButton;
		var endSegmentation;
		var roundEnd;

		var write = {
			score: function() {
				var html = "<font size='5'><b>" + game.score + "</b></font>";
				$("#score").html(html);
			},
			time: function(time) {
				var html = "";
				if(time || time === 0) {
					html = "<font size='5'><b>" + $.i18n.prop('time') + Time.round(time, Time.second) + "</b></font>";
				}
				$("#timeCounter").html(html);
			},
			top: function(text, red) {
				var html = "<font size='5'><b>" + text;
				if(red !== undefined) {
					html += "<font color='red'>" + red + "</font>";
				}
				html += "</b></font>";

				$("#topMessage").html(html);
			},
			error: function(message) {
				$("#onError span").text(message);
				$("#onError").show();
				$("#pageheader").hide();
				$("#mainPage").hide();
			},
			canvasMessage: function(message) {
				var html = "<font size='5'><b><pre id='canvasPre'>" + message + "</pre></b></font>";
				$("#canvasMessage").show();
				$("#canvasMessage").html(html);
			}
		};
		write.score();
		
		if (!game.matchStarted) write.top($.i18n.prop('waiting'));

		var setColor = function(c) {
			tool.color = c;
			communicator.send({
				type: 'change',
				size: tool.size,
				color: tool.color,
				name: user.name,
				role: game.role
			});
		};

		var setSize = function(s) {
			tool.size = s;
			communicator.send({
				type: 'change',
				size: tool.size,
				color: tool.color,
				name: user.name,
				role: game.role
			});
		};

		var hud = {
			canvas: $("#hud"),
			context: $("#hud")[0].getContext("2d"),
			tools: {
				pen: {
					enabled: "assets/images/UI/Controls/pencil.png",
					disabled: "assets/images/UI/Controls/pencilD.png",
					size: { x: 70, y: 70 },
					position: { x: 0, y: 130 }
				},
				eraser: {
					enabled: "assets/images/UI/Controls/eraser.png",
					disabled: "assets/images/UI/Controls/eraserD.png",
					size: { x: 70, y: 70 },
					position: { x: 0, y: 200 }
				}
			},
			preload: function() {
				$.each(this.tools, function(name, tool) {
					$.each(["enabled", "disabled"], function(key, status) {
						var src = tool[status];
						tool[status] = new Image();
						tool[status].src = src;
					});
				});
			},
			clear: function() {
				this.context.clearRect(0, 0, this.canvas.width(), this.canvas.height());
			},
			paint: function(active) {
				this.clear();
				var self = this;
				$.each(this.tools, function(name, tool) {
					var status = (name === active ? "enabled" : "disabled");
					self.context.drawImage(tool[status], tool.position.x, tool.position.y, tool.size.x, tool.size.y);
				});
			},
			bindClick: function() {
				var self = this;
				//[TODO] Try to use modernizer to solve this issue
				this.canvas.on("click", function (e) {
					var o = relativePosition(e, self.canvas);
					if ((o.y >= 130) && (o.y < 200)) {
						setColor(CONSTANTS.PEN_COLOR);
						setSize(CONSTANTS.PEN_SIZE);
						self.paint("pen");
					} else if ((o.y >= 200) && (o.y <= 270)) {
						setColor(CONSTANTS.ERASER_COLOR);
						setSize(CONSTANTS.ERASER_SIZE);
						self.paint("eraser");
					}
				});
				this.canvas.on("touchstart", function (e) {
					var o = relativePosition(e, self.canvas);
					if ((o.y >= 130) && (o.y < 200)) {
						setColor(CONSTANTS.PEN_COLOR);
						setSize(CONSTANTS.PEN_SIZE);
						self.paint("pen");
					} else if ((o.y >= 200) && (o.y <= 270)) {
						setColor(CONSTANTS.ERASER_COLOR);
						setSize(CONSTANTS.ERASER_SIZE);
						self.paint("eraser");
					}
				});
			},
			unbindClick: function() {
				this.canvas.off("click");
				this.canvas.off("touchstart");
			}
		};
		hud.preload();

		var positions = {
			canvas: $("#positions"),
			context: $("#positions")[0].getContext("2d"),
			init: function() {
				this.context.font = "9px monospace";
				this.context.textAlign = "center";
				this.context.textBaseline = "bottom";
			},
			clear: function() {
				this.context.clearRect(0, 0, this.canvas.width(), this.canvas.height());
			},
			paint: function(player, x, y) {
				this.clear();
				this.context.beginPath();
				this.context.strokeStyle = CONSTANTS.TRACKER_COLOR;
				this.context.arc(x, y, player.size / 2, 0, 2 * Math.PI);
				this.context.stroke();
				this.context.font = "10px sans-serif";
				this.context.fillStyle = CONSTANTS.TRACKER_COLOR;
				this.context.fillText((player.name + "").substring(0, 20), x, y - Math.round(player.size / 2) - 4);
			},
			oldX: 0,
			oldY: 0
		};
		positions.init();

		var task = {
			canvas: $("#task"),
			context: $("#task")[0].getContext("2d"),
			clear: function() {
				this.context.clearRect(0, 0, this.canvas.width(), this.canvas.height());
			},
			drawFrame: function() {
				var width = this.canvas.width();
				var height = this.canvas.height();
				var radius = 50;
				this.context.beginPath();
				this.context.moveTo(radius, 0);
				this.context.lineTo(width - radius, 0);
				this.context.quadraticCurveTo(width, 0, width, radius);
				this.context.lineTo(width, height - radius);
				this.context.quadraticCurveTo(width, height, width - radius, height);
				this.context.lineTo(radius, height);
				this.context.quadraticCurveTo(0, height, 0, height - radius);
				this.context.lineTo(0, radius);
				this.context.quadraticCurveTo(0, 0, radius, 0);
				this.context.closePath();
				this.context.clip();
			},
			fitImage: function(imgWidth, imgHeight) {
				var canvasWidth = this.canvas.width();
				var canvasHeight = this.canvas.height();

				var rectangle = { x: 0, y: 0, width: canvasWidth, height: canvasHeight };
				if(imgWidth < canvasWidth && imgHeight < canvasHeight) {
					rectangle.height = imgHeight;
					rectangle.y = (canvasHeight - rectangle.height) / 2;
					rectangle.width = imgWidth;
					rectangle.x = (canvasWidth - rectangle.width) / 2;
				}
				else {
					var widthScale = canvasWidth / imgWidth;
					var heightScale = canvasHeight / imgHeight;
					if (widthScale < heightScale) {
						rectangle.height = imgHeight * widthScale;
						rectangle.y = (canvasHeight - rectangle.height) / 2;
					} else {
						rectangle.width = imgWidth * heightScale;
						rectangle.x = (canvasWidth - rectangle.width) / 2;
					}
				}

				return rectangle;
			},
			paint: function(image, width, height) {
				this.context.save();
				this.clear();
				this.drawFrame();
				var rectangle = this.fitImage(width, height);
				this.context.drawImage(image, rectangle.x, rectangle.y, rectangle.width, rectangle.height);
				this.context.restore();
			}
		};

		var me = {
			canvas: $("#me"),
			context: $("#me")[0].getContext("2d"),
			init: function() {
				this.context.lineCap = 'round';
				this.context.lineJoin = 'round';
			},
			clear: function() {
				this.context.clearRect(0, 0, this.canvas.width(), this.canvas.height());
			},
			lineTo: function(from, to) {
				this.context.strokeStyle = tool.color;
				this.context.lineWidth = tool.size;
				this.context.beginPath();
				this.context.moveTo(from.x, from.y);
				this.context.lineTo(to.x, to.y);
				this.context.stroke();
			}
		};
		me.init();

		var draws = {
			canvas: $("#draws"),
			context: $("#draws")[0].getContext("2d"),
			init: function() {
				this.context.lineCap = 'round';
				this.context.lineJoin = 'round';
			},
			clear: function() {
				this.context.clearRect(0, 0, this.canvas.width(), this.canvas.height());
			},
			setTool: function(name) {
				if(name === "pen") {
					this.context.globalCompositeOperation = "source-over";
				} else if(name === "eraser") {
					this.context.globalCompositeOperation = "destination-out";
				}
			},
			draw: function(points, size) {
				this.context.lineWidth = size;
				this.context.beginPath();
				if(points[0]) this.context.moveTo(points[0].x, points[0].y);
				var self = this;
				points.forEach(function (point) {
					self.context.strokeStyle = point.color;
					self.context.lineTo(point.x, point.y);
				});
				this.context.stroke();
			}
		};
		draws.init();

		/*****************************UTILITY FUNCTIONS********************************************/

		if (!user.name) {
			user.name = $('#currentNickname').text() ||
				localStorage.getItem("pname") ||
				("iam" + Math.floor(100 * Math.random()));
			localStorage.setItem("pname", user.name);
		}

		// WebSocket
		var communicator = new Communicator($('#paintWebSocket').data('ws'));

		$(communicator.websocket).on({
			open: function(evt) {
				communicator.send({
					type: 'change',
					size: tool.size,
					color: tool.color,
					name: user.name
				});
			},
			close: function(evt) {
				write.error("Connection lost");
			},
			error: function(evt) {
				console.error("error", evt);
			}
		});

		var getPlayer = function(username) {
			for (var i = 0; i < players.length; i++) {
				if (players[i].name.toLowerCase() === username.toLowerCase()) {
					return players[i];
				}
			}
		};

		var deletePlayer = function(username) {
			for (var i = 0; i < players.length; i++) {
				if (players[i].name.toLowerCase() === username.toLowerCase()) {
					delete players[i];
					return;
				}
			}
		};

		var playerExtend = function(message) {
			var username = message.name;
			for (var i = 0; i < players.length; i++) {
				if (players[i].name.toLowerCase() === username.toLowerCase()) {
					players[i] = $.extend(players[i], message);
				}
			}
		};

		//***************************TAKING CARE OF THE LINE STROKES*****************************/
		(function() {
			//$('#mainPage').append("<button id=\"skipTask\"></button><button id=\"endSegmentation\"></button>");
			$('#mainPage').append("<button id=\"endSegmentation\"></button>");
			skipButton = $('#skipTask');
			endSegmentation = $('#endSegmentation');
			skipButton.hide();
			endSegmentation.hide();
			skipButton.on("click", function() {
				$("#warnTag").hide();
				if (game.role === "SKETCHER") {
					if (game.tagging) {
						communicator.send({ type: 'skipTask', timerObject: 'tag' });
					} else {
						communicator.send({ type: 'skipTask', timerObject: 'round' });
					}
				}
			});
			endSegmentation.on("click", function() {
				if (players.length === 1) {
						communicator.send({ type: 'endSegmentation', player: user.name  });
				}
			});
			/*******************************MANAGING THE INCOMING MESSAGES*****************/
			communicator.on("role", function(e, message) {
				game.tagging = false;
				//Fix the drawing style for the player
				draws.setTool("eraser");
				game.guessed = false;
				game.role = message.role;
				game.matchStarted = true;
				var player = getPlayer(user.name);
				player.role = message.role;
				communicator.send({
					type: 'change',
					size: tool.size,
					color: tool.color,
					name: user.name,
					role: game.role
				});
				if (game.role === "SKETCHER") {
					$('#roleSpan').text($.i18n.prop('sketcher'));
					$('#mainPage').removeClass('guesser');
					$('#mainPage').addClass('sketcher');
					//Disable the chat just if we are not in single player mode
					if(players.length!=1)
						$('#talk').attr('disabled', 'disabled');
					else
						endSegmentation.show();
					$('#canvasMessage').hide();
					$("#warnTag").hide();
					skipButton.show();
				} else {
					$('#roleSpan').text($.i18n.prop('guesser'));
					$('#mainPage').removeClass('sketcher');
					$('#mainPage').addClass('guesser');
					$('#talk').removeAttr('disabled');
					skipButton.hide();
					endSegmentation.hide();
					$('#canvasMessage').hide();
					$("#warnTag").hide();
					write.top($.i18n.prop('guess'));
				}
				time.setCountdown("round", CONSTANTS.DEFAULT_ROUND_TIME * Time.second, Time.second, write.time, roundEnd);
				time.setTimer("round");
				time.clearCountdown("tag");
				draws.clear();
				task.clear();
			});

			communicator.on("move", function(e, message) {
				positions.paint(getPlayer(message.name), message.x, message.y);
			});

			communicator.on("task", function(e, message) {
				game.tagging = false;
				game.guessWord = message.tag;
				
				$('#canvasMessage').hide();
				$("#warnTag").hide();
				if (game.role === "SKETCHER") {
					hud.paint("pen");
					hud.bindClick();
					write.top($.i18n.prop('draw'), game.guessWord);
				} else if (game.guessed) {
					write.top($.i18n.prop('guessed'), game.guessWord);
				} else {
					write.top($.i18n.prop('solution'), game.guessWord);
				}
				game.taskImage = new Image();
				game.taskImage.src = message.image;
				//Wait for the image to be loaded before drawing it to canvas to avoid errors
				$(game.taskImage).on("load", function () {
					if(((game.role == "ROUNDCHANGE") || (game.role == "SKETCHER")||(game.guessed == true))){
                        task.paint(game.taskImage, message.width, message.height);
                    }
                    else{
                        task.clear();
                    }
				});
			});

			communicator.on("tag", function(e, message) {
				game.tagging = true;
				game.matchStarted = true;
				time.setCountdown("tag", CONSTANTS.DEFAULT_ASK_TIME * Time.second, Time.second, write.time, roundEnd);
				game.role = message.role;
				$('#canvasMessage').hide();
				if (message.role === "SKETCHER") {
					$('#talk').removeAttr('disabled');
					skipButton.show();
					write.top($.i18n.prop('asktagsketcher'));
					draws.clear();
					game.taskImage = new Image();
					game.taskImage.src = message.image;
					$(game.taskImage).on("load", function () {
						task.paint(game.taskImage, message.width, message.height);
					});
					$("#warnPre").html($.i18n.prop('warnTag'));
					$("#warnTag").show();
					$("#warnTag").click(function() {
						$(this).hide();
					});
				} else {
					$('#talk').removeAttr('disabled');
					skipButton.hide();
					endSegmentation.hide();
					write.top($.i18n.prop('asktag'));
					draws.clear();
					draws.clear();
					game.taskImage = new Image();
					game.taskImage.src = $('#questionMark').attr("src");
					write.canvasMessage($.i18n.prop('sketchertagging'));
					$(game.taskImage).on("load", function () {
						task.paint(game.taskImage, $('#questionMark').attr("rwidth"), $('#questionMark').attr("rheight"));
					});
				}
			});

			communicator.on("points", function(e, message) {
				if (message.name === user.name) {
					game.score += message.points;
					write.score();
					game.guessed = true;
					skipButton.hide();
					endSegmentation.hide();
				}
			});

			communicator.on("timeChange", function(e, message) {
				if (time.getCountdown(message.timeObject) > (message.amount * Time.second)) {
					time.changeCountdown(message.timeObject, message.amount * Time.second);
				}
			});

			communicator.on("showImages", function(e, message) {
				game.role = "ROUNDCHANGE";
				skipButton.hide();
				endSegmentation.hide();
				time.clearCountdown("round");
				time.setCountdown("round", message.seconds * Time.second, Time.second, write.time, roundEnd);
				$('#mainPage').removeClass('sketcher');
				$('#mainPage').addClass('guesser');
			});

			communicator.on("leaderboard", function(e, message) {
				game.role = "ENDED";
				$("#warnTag").hide();
				$('#canvasMessage').hide();
				$('#mainPage').removeClass('guesser');
				$('#mainPage').removeClass('sketcher');
				skipButton.hide();
				endSegmentation.hide();
				//Clear all the canvas and draw the leaderboard
				draws.clear();
				task.clear();

				//Disable the chat
				$('#talk').attr('disabled', 'disabled');
				var results="";
				for (var i = 0; i < message.playersNumber; i++) {
					results+=":"+message.playerList[i].name+":"+message.playerList[i].points;
				}
				//Display the leaderboard page with the results
				jsRoutes.controllers.Sketchness.leaderboard(user.name,results).ajax({
					success: function(data) {
						$("#mainPage").html(data);
					},
					error: function() {
					  alert("Error!")
					}
				  })

			});
			
			communicator.on("loading", function(e, message) {
				write.top($.i18n.prop('matchstarting'));
			});

			communicator.on("trace", function(e, message) {
				var player = getPlayer(message.name);
				if (player.color === CONSTANTS.ERASER_COLOR) {
					draws.setTool("eraser");
				} else {
					draws.setTool("pen");
				}
				draws.draw(message.points, player.size);

				// clear local canvas if synchronized
				if (message.name === user.name && game.traceNum === message.num) {
					me.clear();
				}
			});

			communicator.on("change", function(e, message) {
				var player = getPlayer(message.name);
				if (player === undefined) {
					player = players[players.length] = message;
				}
				playerExtend(message);
			});

			communicator.on("disconnect", function(e, message) {
				//[TODO]
				//deletePlayer(message.username);
			});

			roundEnd = function() {
				if (game.role === "SKETCHER") {
					hud.clear();
					hud.unbindClick();
				}
				positions.clear();
				write.time(null);
				communicator.send({ type: 'roundEnded', player: user.name });
			};

		})();

		/*************************DRAWING PANEL**********************/

		//Return the current position of the cursor within the specified element
		var relativePosition = function(event, element) {
			element=element[0];
			var offsetX = 0, offsetY = 0;
 
			// Compute the total offset
			if (element.offsetParent !== undefined) {
				do {
				  offsetX += element.offsetLeft;
				  offsetY += element.offsetTop;
				} while ((element = element.offsetParent));
			}
	
			if(!isMobile) {
				return {
					x: (event.pageX - offsetX),
					y: (event.pageY - offsetY)
				};
			}
			else {
				//Touchend does not have the position of when we lifted our finger, so we can keep track of the last position
				if(event.originalEvent.type!="touchend") {
					if(typeof event.originalEvent.touches != 'undefined') {
						positions.oldX = event.originalEvent.touches[0].pageX - offsetX;
						positions.oldY = event.originalEvent.touches[0].pageY - offsetY;
					}
				}
				return {
					x: (positions.oldX),
					y: (positions.oldY)
				};
			}
		};

		// The "me" canvas is where the sketcher draws before sending the update status to all the other players
		(function() {
			var pressed;
			var position;

			var points = [];
			var lastSent = 0;

			//Add the current point to the list of points to be sent

			var addPoint = function(x, y, size, color) {
				points.push({
					x: x,
					y: y,
					size: size,
					color: color
				});
			};

			//Send the points to the server as a trace message. It sends the points, the number of the trace sent, the name of the player that has sent the trace

			var sendPoints = function() {
				lastSent = Date.now(); //Refresh the countdown timer
				var gameTimer = time.getTimer("round");
				communicator.send({
					type: "trace",
					points: points,
					num: game.traceNum,
					name: user.name,
					time: gameTimer
				});
				points = [];
			};

			//Send the tracking position of the player that is drawing, sending his current position and name

			var sendMove = function(x, y) {
				lastSent = Date.now();
				communicator.send({
					type: "move",
					x: x,
					y: y,
					name: user.name
				});
			};

			//Can we send? If the current time - the last update is bigger than the treshold, we can send the packets

			var canSendNow = function() {
				if (!game.tagging)
					return Date.now() - lastSent > CONSTANTS.MIN_SEND_RATE;
				else
					return false;
			};


			//Handle the mouse movement when drawing

			//Add the event listeners to handle movements, mouse up and mouse down, eventually supporting also mobile devices
			$(document).on((isMobile ? "touchstart" : "mousedown"), function(e) {
				//If the player is a sketcher, update the mouse pressed status to send his traces
				if (game.role === "SKETCHER" && !game.tagging) {
					var o = relativePosition(e, me.canvas);
					position = o;
					addPoint(o.x, o.y, tool.size, tool.color);
					pressed = true;
				}
			});

			$("#viewport").on((isMobile ? "touchmove" : "mousemove"), function(e) {
				e.preventDefault();
				//Get the current position with respect to the canvas element we want to draw to
				var o = relativePosition(e, me.canvas);
				//If the mouse is pressed and the player is a sketcher
				if (pressed && game.role === "SKETCHER") {
					//Draw the local line
					me.lineTo(position, o);
					//Add the points to the points to be sent
					addPoint(o.x, o.y, tool.size, tool.color);
					//We have created a trace
					++(game.traceNum);
					//Can we send the batch of points?
					if (canSendNow()) {
						sendPoints();
						sendMove(o.x, o.y);
						addPoint(o.x, o.y, tool.size, tool.color);
					}
				} else {
					//The mouse is not pressed, can we just send the position of the player?
					if (canSendNow() && game.role === "SKETCHER") {
						sendMove(o.x, o.y);
					}
				}
				position = o;
			});

			$(document).on((isMobile ? "touchend" : "mouseup"), function(e) {
				//If the player is the sketcher, send the last trace and disable the drawing function
				if (game.role === "SKETCHER" && !game.tagging) {
					var o = relativePosition(e, me.canvas);
					me.lineTo(position, o);
					addPoint(o.x, o.y, tool.size, tool.color);
					addPoint(o.x, o.y, tool.size, "end");
					position = o;
					sendPoints();
					pressed = false;
				}
			});

		})();
	});
});
