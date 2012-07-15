(
MIDIClient.init;
//There are three possibilities to connect for example to the first device:
//MIDIIn.connect(0, MIDIClient.sources.at(0));
//MIDIIn.connect(0, MIDIClient.sources.at(0).uid);
MIDIIn.connectAll;
~mout = MIDIOut(0);


/// warp intens
~warpdur = 1;
)

MIDIdef.noteOn(\fog, { |msg|
	Patcher.all.at(\waldstock).busesForMethod(\fog)[0].set(1);
}, 1);
MIDIdef.noteOff(\fogoff, { |msg|
	Patcher.all.at(\waldstock).busesForMethod(\fog)[0].set(0);
},1);
MIDIdef.noteOn(\blitz, { |msg|
	Patcher.all.at(\waldstock).busesForMethod(\blitz)[0].set(1);
}, 5);
MIDIdef.noteOff(\blitzoff, { |msg|
	Patcher.all.at(\waldstock).busesForMethod(\blitz)[0].set(0);
},5);

MIDIdef.noteOn(\test, { arg ...args; args.postln});

/// bck onoff
~bck = false;
MIDIdef.noteOn(\bck, { |msg|
	~bck.postln;
	if(~bck.not, {
		~bck = true;
		Ndef(\bck).stop;
		m[\bck].set(\amp, 0);
		~mout.noteOff(0, 8);
	}, {
		~bck = false;
		Ndef(\bck).play;
		m[\bck].set(\amp, 1);
		~mout.noteOn(0, 8);
	})
}, 8);
/// bck intens
MIDIdef.cc(\bcki, { |val|
	Ndef(\bck).set(\int, val/127);
	m[\bck].set(\int, val/127);
}, 8);

/// warp onoff
~warp = false;
MIDIdef.noteOn(\warp, { |msg|
	if(~warp.not, {
		~warp = true;
		Pdef(\warp).stop; Pdef(\warpaudio).stop;
		~mout.noteOff(0, 4);
	}, {
		~warp = false;
		Pdef(\warp).play; Pdef(\warpaudio).play;
		~mout.noteOn(0, 4);
	})
}, 4);
/// warp intens
MIDIdef.cc(\warpi, { |val|
	~warpdur = val.linlin(0, 127, 1/4, 8).postln;
}, 4)
