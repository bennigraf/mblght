mblght
======

This is my try on using SuperCollider DSP magic (on scsynth) to synthesize signals which can control various lighting fixtures using the Open Lighting Architecture (OLA) to play out Artnet and/or DMX signals.

Documentation
-------------

...is really spare until now. In short: Install OLA/olad and get it running, use LGui.new() to do some basic setup tasks, find out the rest yourself. Or have fun with this little snippet:

	SynthDef(\test, {
		var color = SinOsc.kr({0.4.rand}!3 + 0.1, pi.rand, mul: 0.5, add: 0.5);
		Patcher.default.busesForMethod(\color).do({ |bus, i|
			Out.kr(bus, (color - (0.02 * i)).fold(0, 1));
		});
	}).play;

More documentation is hopefully to come, but until then you find more examples in /misc, /scenes and /waldstock.

Examples
--------

Some colorful pictures (it's about the lighting, not the projection, which was part of the Stereo Tam Tam gig):
https://www.facebook.com/media/set/?set=a.399699220085179.103947.172943649427405&type=1

Two short videos demonstrating what's possible but using alternate visualisations (not the real thing):
https://www.facebook.com/photo.php?v=10151593592457715
https://www.facebook.com/photo.php?v=10151588297902715