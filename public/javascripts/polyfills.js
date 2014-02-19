// TODO: chose which functionality support and maybe use modernizr for tests

if (!window.requestAnimationFrame) {
	window.requestAnimationFrame = window.webkitRequestAnimationFrame ||
		window.mozRequestAnimationFrame ||
		window.oRequestAnimationFrame ||
		window.msRequestAnimationFrame ||
		function (callback) {
			window.setTimeout(callback, 1000 / 60);
		};
}

if(!window.WebSocket && window.MozWebSocket) {
	window.WebSocket = window.MozWebSocket;
}

if (!Function.prototype.bind) {
	Function.prototype.bind = function(context) {
		if(typeof this !== "function") {
			throw new TypeError("Function.prototype.bind called on incompatible object");
		}

		var self = this;
		var args = Array.prototype.slice.call(arguments, 1);
		return function() {
			return self.apply(context, args.concat(arguments));
		};
	};
}

if (!Date.prototype.now) {
	Date.prototype.now = function() {
		return +(new Date());
	};
}

if(false) { // TODO: I'm not sure if it's useful, neither where to place it..think about it!
	var typeOf = function(obj) {
		if (obj === null) return "null";
		else {
			var type = typeof(obj);
			if (type != "object") return type;
			else {
				var toString = Object.prototype.toString;
				var str = toString.apply(obj);
				if(str == "[object String]") return "string";
				else if (str == "[object Array]") return "array";
				else if (str == "[object Date]") return "date";
				else if (obj instanceof RegExp) return "regexp";
				else return "object";
			}
		}
	};
}
