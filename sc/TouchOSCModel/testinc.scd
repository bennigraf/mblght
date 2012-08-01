x = NodeProxy.control(s).source_({
/*	Out.kr(0, SinOsc.kr(1));*/
	SinOsc.kr(1);
})
x.free
x.dump
x.bus.get


x = Bus.control(s, 1)
y = Bus.control(s, 1)
a = { Out.kr(x, SinOsc.kr(1)) }.play
b = { Out.kr(y, LFSaw.kr(1/2)) }.play

y.get

c = {
	In.kr([x.index, y.index]).poll
}.play
c.free

a = NodeProxy.control(s).source_({ SinOsc.kr(1) })
b = NodeProxy.control(s).source_({ LFSaw.kr(1/2) })

c = NodeProxy.control(s).source_({
	[\arga.kr(0), \argb.kr(0)].poll;
})

c.map(\arga, a)
c.map(\argb, b)
c.unmap(\arga)
c.free