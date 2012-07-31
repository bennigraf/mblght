TouchControl.replAddr = NetAddr("padrick.local", 5001);
TouchControl.replAddr = NetAddr("192.168.20.1", 5001);

x = TouchLFO(1);
x.bus.getSynchronous
TouchColor.lfos.add(x)

~touchctls = List();
6.do({ |n|
	~touchctls.add(TouchLFO(n+1));
});
~touchctls[0].bus.index

~touchctls.do({|ctl|
	TouchColor.lfos.add(ctl);
});


y = TouchColor(1)


y.controls.r.bus.get

y.colorNode.set(\rbus, y.controls.r.bus.index)
