mblght
======

This is my try on using SuperCollider DSP magic (running on scsynth) to synthesize signals which can control various lighting fixtures using the Open Lighting Architecture (OLA) to play out Artnet and/or DMX signals.

A VIDEO of the system in use is available here: https://vimeo.com/57644362

Further information is available here: http://www.bennigraf.de/mblght/

Documentation
-------------

You find the documentation in german in the file mblght-Doku-pdf.pdf. English documentation isn't available yet unfortunately.

In short: Install OLA/olad and get it running, use LGui.new() to do some basic setup tasks and start with this little snippet:

	SynthDef(\test, {
		var color = SinOsc.kr({0.4.rand}!3 + 0.1, pi.rand, mul: 0.5, add: 0.5);
		Patcher.default.busesForMethod(\color).do({ |bus, i|
			Out.kr(bus, (color - (0.02 * i)).fold(0, 1));
		});
	}).play;

You can find more examples in /misc, /scenes and /waldstock.

Examples
--------

A couple of videos have been made where mblght was put to good use!
https://vimeo.com/57644362 -- at the Waldstock Festival in Bad Waldsee, used for stage & ambient lighting

https://vimeo.com/70421823 -- at the SoNaFe in Ulm, used for ambient lighting

https://vimeo.com/51610988 -- more simple test run in a barn

Some colorful pictures (it's about the lighting, not the projection, which was part of the Stereo Tam Tam gig):
https://www.facebook.com/media/set/?set=a.399699220085179.103947.172943649427405&type=1

Two short videos demonstrating what's possible but using alternate visualisations (not the real thing):
https://www.facebook.com/photo.php?v=10151593592457715
https://www.facebook.com/photo.php?v=10151588297902715

Notice
------

This software was created as part of my Bachelor's Thesis at the Institute for Musicology and Music Informatics (IMWI) at the University of Music, Karlsruhe (Germany).
