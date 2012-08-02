/*TouchControl.replAddr = NetAddr("padrick.local", 5001);*/
TouchControl.replAddr = NetAddr("192.168.20.1", 5001);

~touchctls = List();
6.do({ |n|
	~touchctls.add(TouchLFO(n+1));
});
~touchctls[0].bus.index

~touchctls.do({|ctl|
	TouchColor.lfos.add(ctl);
});

~touchclrs = List();
6.do({ |n|
	~touchclrs.add(TouchColor(n+1));
});

~touchclrs.size

add(TouchColor(6))

LGui()

Patcher.default.havefun
Patcher.default.enoughfun

OSCFunc.trace(true)

/////////////////////supertouch
/*
1. create lfos
2. create colors
3. TouchLight with lfos and colors
*/
LGui();
(Routine.run({
	defer{LGui()};
	0.5.wait;
	TouchControl.replAddr = NetAddr("192.168.20.1", 5001);

	~touchctls = List();
	6.do({ |n|
		~touchctls.add(TouchLFO(n+1));
	});
	1.wait;
	
	~touchctls.do({|ctl|
		TouchColor.lfos.add(ctl);
	});

	~touchclrs = List();
	6.do({ |n|
		~touchclrs.add(TouchColor(n+1));
	});
	1.wait;

	TouchLight.lfos_(~touchctls);
	TouchLight.colors_(~touchclrs);
	TouchLight.patcher_(\waldstock);
	x = TouchLight()
}))
x.mainlights[0][\synth].bus.get
x.mainlights[0][\synth].bus.getSynchronous
x.mainlights[0][\synth].get(\param1).bus.get
x.mainlights[0][\paramsynths][0].get(\lfo6)
x.mainlights[0][\paramsynths][0].set(\lfo1, 1)
x.mainlights[0][\paramsynths][0].bus.getSynchronous
x.mainlights[0][\synth].set(\color1, 0.5)
x.mainlights[0][\synth].map(\param1, x.mainlights[0][\paramsynths][0])

x.mainlights[0][\paramsynths][0].set(\lfobus2, ~touchctls[0].bus.index)
~touchctls[0].bus.index



x = TouchControl(\xy, '/main/pos', false)
x.action_({|val| val.postln;})
x.set([0.3, 0.8], true)
x.val


x = TouchControl(\button, '/main/selectchan', false)
z = List()
3.do({ |i|
	z.add(TouchControl(\led, '/main/chan'++(i+1)++'sel', false))
});
y.set(1)
z[2].set(1)

e = ()
e[\test][\some] = 1