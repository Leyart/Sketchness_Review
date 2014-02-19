define(["Class","Countdown"], function(Class, Countdown){

	/**
	 * The handler of time infos and timed events
	 * (All times are measured in milliseconds)
	 */
	return new Class({
		_name: "Time",

		/**
		 * Initializes a start timestamp and the collections
		 */
		_init: function() {
			this.start = Date.now();
			this.countdowns = {};
			this.timers = {};
		},

		_proto: {
			/**
			 * Sets up a countdown starting at the current time
			 *
			 * @param name :String The identifying name of the countdown
			 * @param duration :Number The duration of the countdown
			 * @param step :Number The step between each tick
			 * @param update :Function([tm,i]) The function to notify the tick
			 *        Receives as parameters the remaining time (tm) and the current iteration (i)
			 * @param event :Function The function to run at the end
			 */
			setCountdown: function(name, duration, step, update, event) {
				this.countdowns[name] = new Countdown(name, duration, step, update, event);
			},

			/**
			 * Retrieves the time left to a countdown
			 *
			 * @param name :String The identifying name of the countdown
			 *
			 * @return :Number The time left or undefined if countdown is not set
			 */
			getCountdown: function(name) {
				return this.countdowns[name] && this.countdowns[name].left();
			},

			/**
			 * Changes the end time to the current time plus the given duration
			 *
			 * @param name :String The identifying name of the countdown
			 * @param duration :Number The new duration from now
			 *
			 * @throws :Error If the coundown is already expired
			 */
			changeCountdown: function(name, duration) {
				if(this.countdowns[name]) this.countdowns[name].change(duration);
			},

			/**
			 * Stops and delete a countdown
			 *
			 * @param name :String The identifying name of the countdown
			 */
			clearCountdown: function(name) {
				if(this.countdowns[name]) {
					this.countdowns[name].clear();
					delete this.countdowns[name];
				}
			},

			/**
			 * Sets a named timer starting at current time
			 *
			 * @param name :String The identifying name of the timer
			 */
			setTimer: function(name) {
				this.timers[name] = Date.now();
			},

			/**
			 * Retrieves the elapsed time since the start of a timer
			 *
			 * @param name :String The identifying name of the timer
			 *
			 * @return :Number The elapsed time or undefined if timer is not set
			 */
			getTimer: function(name) {
				return this.timers[name] && (Date.now() - this.timers[name]);
			},

			/**
			 * Deletes a timer
			 *
			 * @param name :String The identifying name of the timer
			 */
			clearTimer: function(name) {
				if(this.timers[name]) {
					delete this.timers[name];
				}
			}
		},

		_static: {
			/**
			 * Second unit in milliseconds
			 */
			second: 1000,

			/**
			 * Minute unit in milliseconds
			 */
			minute: 60000,

			/**
			 * Hour unit in milliseconds
			 */
			hour: 3600000,

			/**
			 * Day unit in milliseconds
			 */
			day: 86400000,

			/**
			 * Rounds a time in milliseconds to the given unit
			 * with the required precision.
			 *
			 * @param time :Number The time to round
			 * @param unit :Number The result unit
			 * @param precision :Number The number of decimal digits
			 *
			 * @return :Number The rounded time
			 */
			round: function(time, unit, precision) {
				var offset = precision ? Math.pow(10, precision) : 1;
				return Math.round(time / unit * offset) / offset;
			}
		}
	});

});
