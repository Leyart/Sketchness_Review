define(["jquery"], function($) {

	/**
	 * Class factory
	 */
	var Class = {
		_name: "Class",

		/**
		 * Creates a class on the given model
		 *
		 * @param model :Object The model that define the class, with some of the following properties
		 *     @property _name :String The name of the class (required)
		 *     @property _traits :Array The traits to implement
		 *     @property _init :Function The constructor function
		 *     @property _proto :Object The properties to implement
		 *     @property _static :Object The static properties accessible on the class
		 *
		 * @return :Function The class ready to be used
		 */
		_init: function(model) {
			var cl = model._init || function() {};

			if(model._static) $.extend(cl, model._static);
			cl._name = model._name;

			if(model._traits) {
				$.extend.apply($,[cl.prototype].concat(model._traits));
			}
			if(model._proto) $.extend(cl.prototype, model._proto);

			if(cl.prototype.constructor !== cl) cl.prototype.constructor = cl;

			return cl;
		}
	};

	return Class._init(Class);

});
