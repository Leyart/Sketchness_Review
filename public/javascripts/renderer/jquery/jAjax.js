/*
 * jAjax. The jQuery plugin for unifying ajax requests
 *
 * Copyright (c) 2012 Homeria Open Solutions S.L.
 * http://www.homeria.com
 *
 * Launch  : August 2012
 */
;(function( $ ){

$.jAjaxRequestQueueCont = 0;
$.jAjaxRequestQueue = [];

function on_success (xhr,status)
{

}

function jAjaxError(response)
{
	console.log("AJAX ERROR:" + response);
}

function on_complete (xhr,status)
{

}

function on_error (xhr,status)
{
	if ( xhr == null || xhr.responseText == null || xhr.responseText.length == 0)
	{
		if (xhr != null && xhr.status === 0)
			console.log("Ajax aborted");
		else
			jAjaxError();
		return;
	}
	try{
		jAjaxError( xhr.responseText );
	}
	catch(e){
		jAjaxError();
	}
}	

$.jAjax = function(options){
	
	var defaults = {
		onSuccess: on_success,
		onError: on_error,
		onComplete: on_complete,
		data: null,
		headers : null,
		dataType : "json",
		processData: true,
		showLoading: true,
		enqueue : true,
		savingOperation: false, //if true, user is propmpt to not leave the page when a response is still loading
		loadingMessage: "Loading...", //Saving... by default if savingOperation == true
		parameters: [[]],
		method: "GET"
	};
	if ( options.savingOperation )
	{
		defaults.loadingMessage = "Saving...";
	}
	var options = $.extend(defaults, options);
	
	if ( options.savingOperation && options.enqueue )
	{
		if ( $.jAjaxRequestQueueCont > 0 )
		{
			$.jAjaxRequestQueueCont++;
			$.jAjaxRequestQueue.push(options);
			return;
		}
		$.jAjaxRequestQueueCont++;
	}
	
	$.ajax({
		type: options.method,
		url: options.url,
		beforeSend: function(xhr){
			if ( options.showLoading )
			{
				var ajax_loading = $("#loadingLabel");
				$.jAjax.count = $.jAjax.count == undefined ? 1 : $.jAjax.count+1;
				if (ajax_loading.length == 0) {
					$("body").append("<div id='loadingLabel' style='display:none'>"+
						"<div class='popupPanel'><span class='loadingLabelText'>"+ options.loadingMessage +"</span><span>Please Wait</span></div></div>");
                    ajax_loading = $("#loadingLabel");
                }
                ajax_loading.find(".loadingLabelText").text(options.loadingMessage);
				if (options.savingOperation)
                    ajax_loading.addClass("savingOperation");
				else
                    ajax_loading.removeClass("savingOperation");
                ajax_loading.show();
			}
		  	if ( options.data != null )
				xhr.setRequestHeader("Content-Type", "application/json");
		},
		complete: function(xhr,status){
			if ( options.savingOperation )
			{
				$.jAjaxRequestQueueCont--;
				if ( $.jAjaxRequestQueueCont > 0 ){
					var opts = $.jAjaxRequestQueue.shift(options);
					opts.enqueue = false;
					$.jAjax(opts);
				}
			}
			if ( options.showLoading )
			{
				$.jAjax.count--;
				if ( $.jAjax.count <= 0)
					$("#loadingLabel").hide();
			}
			if (status == "success")
				options.onSuccess(xhr,status);
			else
				options.onError(xhr,status);
			options.onComplete(xhr,status);
			
		},
		processData: options.processData,
		headers : options.headers,
		data: options.data
	});
};

})( jQuery );