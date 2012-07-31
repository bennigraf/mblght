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