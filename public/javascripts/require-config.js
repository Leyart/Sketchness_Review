require.config({
	baseUrl: "assets/javascripts",
	paths: {
		"jquery": "lib/jquery",
		"i18n": "lib/jquery.i18n.properties",
		"mousewheel": "lib/jquery.mousewheel",
		"jscrollpane": "lib/jquery.jscrollpane",
		"popup": "lib/popUp",
	},
	shim: {
		"jquery": {
			exports: "$"
		},
		"i18n": {
			deps: ["jquery"],
			exports: "$.i18n"
		},
		"jscrollpane": {
			deps: ["jquery", "mousewheel"],
			exports: "$.fn.jScrollPane"
		},
		"popup": {
			deps: ["jquery"],
			exports: "$.fn.popup"
		}
	}
});
