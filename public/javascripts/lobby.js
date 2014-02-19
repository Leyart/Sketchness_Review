require(["Communicator", "jquery", "popup", "jscrollpane"], function(Communicator, $) {

	$(function() {

		// Show error message
		var setError = function(message) {
			$("#onError span").text(message);
			$("#onError").show();
			$("#pageheader").hide();
			$("#mainPage").hide();
		};

		$("#gameListBody").jScrollPane({
			showArrows: true,
			mantainPosition: true,
			stickToBottom: true,
			animateScroll: true,
			horizontalGutter: 10
		});


		//Popup for the game creation panel
		var options = { width: 350, height: 350, top: 200, left: 100 };
		$('.default_popup').popup(options);

		var chosenId = undefined;

		// WebSocket
		var communicator = new Communicator($('#lobbyWebSocket').data('ws'));

		$(communicator.websocket).on({
			close: function(evt) {
				setError("Connection lost");
			},
			error: function(evt) {
				setError(evt);
			}
		});

		//Set the game ID when the user select a row in the
		//game list; highlight the chosen row
		$('#gameList').on('click', 'tr', function(event) {
			//Pick the chosen row
			var $tr = $(this).closest('tr');
			//If it is not the head row of the table
			if($tr.attr('class') != 'head')
			{
				//Get the id of the game
				var id = $tr.attr('name');
				var $currentRoom = $("#roomId");
				$currentRoom.attr('value',id);
				//Remove the highlighting from the other rows
				$('#gameList').children("tbody")
							  .children(".jspContainer")
							  .children(".jspPane")
							  .children("tr")
							  .removeClass("highlight");
				//Highlight the curront row
				$tr.addClass("highlight");
			}
		});

		communicator.on("updateList", function(e, data) {
			var $gameInstance = $("#" + data.id);
			//Modify existing row
			if($gameInstance.length > 0) {
				//If the room is empty, it means that it has been destroyed, remove it from the list
				if(data.currentPlayers <= 0 || data.visible == false) {
					$gameInstance.remove();
				} else { //Otherwise update the player count
					var html = '';
					html += '<td>' + data.roomName + '</td><td>' + data.currentPlayers +'/' + data.maxPlayers + '</td>';
					$gameInstance.html(html);
				}
			} else { //Otherwise add a new game to the list
				if(data.visible != false) {
					var html = '';
					html += '<tr id=' + data.id + ' name=' + data.roomName + '><td>' + data.roomName + '</td><td>' + data.currentPlayers + '/' + data.maxPlayers + '</td></tr>';
					var paneApi = $("#gameListBody").data('jsp');
					paneApi.getContentPane().append(html);
					paneApi.reinitialise();
					paneApi.scrollToBottom();
					//$("#gameList tbody").append(html);
				}
			}
		});

	});

});
