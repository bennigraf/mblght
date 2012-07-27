TouchControl.replAddr = NetAddr("padrick.local", 5001);
x = TouchLFO(1)
x.myPhasor.bus.getSynchronous
x.lfo.set(\phasebus, x.myPhasor.bus)
TouchLFO.phasor.bus.getSynchronous

x.types.dump

x.bus.getSynchronous
x.lfo.bus.getSynchronous

x.wavetableBuf.get(14, {|val| val.postln })
x.wavetableBuf.set(13, 1)
x.wavetableBuf.sendCollection(Signal.sineFill(20, [1]).as(Array)/2+0.5)
x.lfo.set(\wtBuf, 0)

OSCFunc.trace(false)


x = {
	LFNoise0.kr(4).poll
}.play


Ndef.kr(\test, 1)
Ndef(\test).source = {
	var env = InterplEnv(\wave.kr(0!5).poll, 1/4!4);
	IEnvGen.kr(env, LFSaw.kr(1/3,0, 0.5, 0.5)).poll;
}

Ndef(\test).setn(\wave, {1.0.rand}!4)
Ndef(\test).setn(\wave, [0, 1, 0, 0.5, 0])

Routine.run({
6.do({ |n|
	TouchLFO(n+1);
	s.sync;
})
})


x = TouchLFO(1)
y = TouchLFO(2)
z = TouchLFO(3)
a = TouchLFO(4)
b = TouchLFO(5)
c = TouchLFO(6)

y.lfo.controlNames
y.lfo.set(\wtBuf, 1)

~a = Buffer.alloc(s, 20)
x = NodeProxy.control(s, 1)
x.source = { \wtBuf.kr(0).poll }
x.source = { LFSaw.kr(1/3).poll; }
x.source = { BufRd.kr(1, \wtBuf.kr(0).poll, LFSaw.kr(1/3, 0, 0.5, 0.5) * 20, 1, 2); }
x.free
x.controlNames
~a.set(12, 1)
~a.sendCollection(0!20)

~b = Buffer.alloc(s, 20)
~b.set(3, 1)
~b.set(8, 1)
~b.sendCollection(0.5!20)

x.set(\wtBuf, 1)



