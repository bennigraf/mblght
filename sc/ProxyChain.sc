ProxyChain {
	
	var <nodes;
	var bottomprox, topprox;
	var server;
	
	*new {
		^super.new.init();
	}
	
	init {
		nodes = List();
		bottomprox = nil; topprox = nil;
		server = Server.default;
		this.updateChain;
	}
	
	add { |nodeproxy|
		if(bottomprox.isNil, {
			nodes.add(nodeproxy);
		}, {
			nodes.insert(nodes.size - 1, nodeproxy);
		});
		this.updateChain;
	}
	
	remove { |index|
		var node;
		if(nodes[index].notNil, {
			if(nodes[index] == bottomprox, {
				bottomprox = nil;
			});
			if(nodes[index] == topprox, {
				topprox = nil;
			});
			node = nodes.removeAt(index);
			node.clear;
			this.updateChain;
			^node;
		});
	}
	
	getNodeIndex { |node|
		^nodes.find(List.newUsing([node]));
	}
	
	addFirst { |nodeproxy|
		if(topprox.isNil, {
			nodes.insert(0, nodeproxy);
		}, {
			nodes.insert(1, nodeproxy);
		});
		this.updateChain;
	}
	addLast { |nodeproxy|
		this.add(nodeproxy);
	}
	
	addBefore { |index, nodeproxy|
		if((index > 1) || (index == 0 && topprox.isNil), {
			nodes.insert(index, nodeproxy);
		});
		this.updateChain;
	}
	
	addAfter { |index, nodeproxy|
		if((index < nodes.size && bottomprox.isNil) || (index < (nodes.size - 1)), {
			nodes.insert(index + 1, nodeproxy);
		}, {
			this.add(nodeproxy);
		});
		this.updateChain;
	} 
	
	moveUp { |index|
		if((index > 0 && topprox.isNil) || (index > 1), {
			var node = nodes.removeAt(index);
			nodes.insert(index-1, node);
		}, {
			"cannot do this: node is on top already or another node is sticking there...".postln;
		});
		this.updateChain;
	}
	
	
	moveDown { |index|
/*		if(index < (nodes.size))*/
		var lastindex = nodes.size - 1;
		if((index < (lastindex - 1)) || (index == lastindex && bottomprox.isNil), {
			var node = nodes.removeAt(index);
			nodes.insert(index+1, node);	
		});
		this.updateChain;
	}
	
	stickToBottom { |index|
		// usage: pc.stickToBottom(pc[4]); - should give proxy returned by pc[4] as argument.
		var node;
		if(index.isNil, {
			index = nodes.size-1;
		});
		node = nodes.removeAt(index);
		this.getNodeIndex(node).postln;
		bottomprox = node;
		nodes.add(node);
		this.getNodeIndex(node).postln;
		this.updateChain;
	}
	stickToTop { |index|
		// usage: see above
		var node;
		if(index.isNil, { index = 0 });
		node = nodes.removeAt(index);
		topprox = node;
		this.addFirst(node);
		this.updateChain;
	}
	unStickFromBottom {
		bottomprox = nil;
	}
	unStickFromTop {
		topprox = nil;
	}
	
	
	updateChain {
		// disconnect (unmap) all nodes, reconnect (map inputs) them in order...
		Routine.run({
			nodes.do({ |node, i|
				if(i > 0, {
					node.unmap(\in);
				});
			});
			server.sync;
			nodes.do({ |node, i|
				if(i > 0, { // omit first node...
					node.map(\in, nodes[i - 1]);
				});
			});
			server.sync;
		});
	}
	
}
/*
List.newUsing([1, 2, 3]).insert(1, 1.4)
d = List.newUsing([1, 2, 3, 4]).removeAt(1)
d.removeAt(1)
d[1]

e = d.removeAt(1)
d.insert(2, e)

d.find(List.newUsing([4]))

y = [7, 8, 7, 6, 5, 6, 7, 6, 7, 8, 9];
y.find(8);*/


/*
s.waitForBoot({b = Bus.control(s, 1) });
b.scope
p = ProxyChain()
p.nodes.dump
p.nodes[0].dump
p.nodes[1].dump
p.nodes[2].dump
b.getSynchronous
p.add(NodeProxy().source = { SinOsc.kr(1/8) })
p.add(NodeProxy().source = { Out.kr(b, \in.kr) })
p.stickToBottom(1)
p.add(NodeProxy().source = { \in.kr * LFSaw.kr(2)})
p.updateChain()
p.add(NodeProxy().source = { A2K.kr(Silent.ar) + 0.2 })
p.add(NodeProxy().source = { A2K.kr(Silent.ar) + 0.4 })
p.moveUp(3)
p.moveDown(3)
p.remove(0)
p.remove(2)
p.unStickFromBottom
p.moveUp(2)
p.dump
*/
