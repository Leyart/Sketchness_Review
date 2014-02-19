define(["Class"], function(Class) {

	/**
	 * Holds all the information of a countdown
	 */
	return new Class({
		_name: "Countdown",

		/**
		 * Countdown constructor
		 *
		 * @param name :String The identifying name of the countdown
		 * @param duration :Number The duration of the countdown in milli secs
		 * @param step :Number The step between each tick in ms
		 * @param update :Function([tm,i]) The function to notify each tick (first tick is istantaneous)
		 *        Receives as parameters the remaining time (tm) and the current iteration (i) from 0
		 * @param event :Function The function to run at the end
		 */
		_init: function(name, duration, step, update, event) {
			this.name = name;
			this.end = Date.now() + duration;
			this.update = update;
			this.event = event;
			this.i = 0;
			this.active = true;

			this.interval = window.setInterval(this.tick.bind(this), step);
			this.timeout = window.setTimeout(this.trigger.bind(this), duration);

			this.tick();
		},

		_proto: {
			/**
			 * Return the remaining time before the end of the countdown
			 *
			 * @return :Number The time left
			 */
			left: function() {
				if (this.active) {
					var left = this.end - Date.now();
					if(left > 0) return left;
				}
				return 0;
			},

			/**
			 * Call the update function and increment the tick count
			 * Launched periodically for each step of the countdown
			 */
			tick: function() {
				var tm = this.left();
				if(tm > 0) {
					this.update.call(null, tm, this.i);
					++(this.i);
				}
			},

			/**
			 * Clear the countdown and call update (for the last time) and the final event
			 * Launched when the time expires
			 */
			trigger: function() {
				if(this.active) {
					this.clear();
					this.update.call(null, 0, this.i);
					this.event.call(null);
				}
			},

			/**
			 * Change the end-time to the current-time plus the given duration
			 *
			 * @param duration :Number The new duration from now
			 *
			 * @throws :Error If the coundown is already expired
			 */
			change: function(duration) {
				if (!this.active) {
					throw new Error("Countdown already expired! It cannot be changed.");
				}

				window.clearTimeout(this.timeout);
				this.end = Date.now() + duration;
				this.timeout = window.setTimeout(this.trigger.bind(this), duration);
			},

			/**
			 * Clears the countdown if active
			 */
			clear: function() {
				if(this.active) {
					window.clearInterval(this.interval);
					window.clearTimeout(this.timeout);
					this.active = false;
				}
			}
		}
	});

});
