/*global Viva*/
var colors = [
0x1f77b4ff, 0xaec7e8ff,
0xff7f0eff, 0xffbb78ff,
0x2ca02cff, 0x98df8aff,
0xd62728ff, 0xff9896ff,
0x9467bdff, 0xc5b0d5ff,
0x8c564bff, 0xc49c94ff,
0xe377c2ff, 0xf7b6d2ff,
0x7f7f7fff, 0xc7c7c7ff,
0xbcbd22ff, 0xdbdb8dff,
0x17becfff, 0x9edae5ff
];

function beginRemoveNodesLoop(graph){
   var nodesLeft = [];
   graph.forEachNode(function(node){
       nodesLeft.push(node.id);
   });
   var removeInterval = setInterval(function(){
        var nodesCount = nodesLeft.length;
        if (nodesCount > 0){
            var nodeToRemove = Math.min((Math.random() * nodesCount) << 0, nodesCount - 1);
            graph.removeNode(nodesLeft[nodeToRemove]);
            nodesLeft.splice(nodeToRemove, 1);
        }
        if (nodesCount <= 2500) {
            clearInterval(removeInterval);
        }
    }, 100);
}

function beginAddNodesLoop(graph){
  var i = 0, m = 10, n = 50;
    var addInterval = setInterval(function(){
        graph.beginUpdate();
        for (var j = 0; j < m; ++j){
            var node = i + j * n;
            if (i > 0) { graph.addLink(node, i - 1 + j * n); }
            if (j > 0) { graph.addLink(node, i + (j - 1) * n); }
        }
        i++;
        graph.endUpdate();
        if (i >= n) {
            clearInterval(addInterval);
            setTimeout(function() {
                beginRemoveNodesLoop(graph);
            }, 10000);
        }
    }, 100);
}

var domLabels;
var container = document.body;

function onLoad() {
   var graph = Viva.Graph.graph();
   var layout = Viva.Graph.Layout.forceDirected(graph, {
       springLength : 10,
       springCoeff : 0.0008,
       dragCoeff : 0.02,
       gravity : -1.2
   });
   var graphics = Viva.Graph.View.webglGraphics();
   graphics
       .node(function(node){
           var ui = Viva.Graph.View.webglSquare(1 + Math.random() * 10, colors[(Math.random() * colors.length)]);
            
           //ui.addEventListener('click', function () {
                // toggle pinned mode
           //     layout.pinNode(node, !layout.isNodePinned(node));
           //});

           return ui;
       })
       .link(function(link) {
           return Viva.Graph.View.webglLine(colors[(Math.random() * colors.length) << 0]);
       });

    //domLabels = generateDOMLabels(graph);

    // I'm not quite happy with how events are currently implemented
    // in the library and I'm planning to refactor it. But for the
    // time beings this is how you track webgl-based input events:
    var events = Viva.Graph.webglInputEvents(graphics, graph);
    events.mouseEnter(function (node) {
        console.log('Mouse entered node: ' + node.id);
    }).mouseLeave(function (node) {
        console.log('Mouse left node: ' + node.id);
    }).dblClick(function (node) {
        console.log('Double click on node: ' + node.id);
    }).click(function (node) {
        console.log('Single click on node: ' + node.id);
    });

    var renderer = Viva.Graph.View.renderer(graph,
       {
           layout     : layout,
           graphics   : graphics,
           container  : document.getElementById('graph1'),
           renderLinks : true
       });

   renderer.run(50);
    //graph.addLink(1, 2)
   //beginAddNodesLoop(graph);
   l = layout;

    var blockchainWebSocket = $.simpleWebSocket(
      {
        url: 'ws://' + window.location.href.split('//')[1].split(':')[0] + ':18080'
        , dataType: 'json'
      }
    );

    var maxNodes = 5000;
    var i = 0;

    // first we generate DOM label for each graph node. Be cautious
    // here, since for large graphs with more than 1k nodes, this will
    // become a bottleneck.
    //domLabels = generateDOMLabels(graph);

    function generateDOMLabels(graph) {
      // this will map node id into DOM element
      var labels = Object.create(null);
      graph.forEachNode(function(node) {
        var label = document.createElement('span');
        label.classList.add('node-label');
        label.innerText = node.id;
        labels[node.id] = label;
        container.appendChild(label);
      });
      // NOTE: If your graph changes over time you will need to
      // monitor graph changes and update DOM elements accordingly
      return labels;
    }

    // reconnected listening
    blockchainWebSocket.listen(function(message) {
    try {
        if ( message['op'] == 'pong' ) {
            return;
        } else if (message["op"] == "hourly_volume") {
            buildChart(message["rows"]); // Defined in ./js/hourlyVolume.js
        } else {
            var txAddress = txAddress = message['x']['hash'];

            graph.beginUpdate();

            for (var j = 0; j < message['x']['inputs'].length; ++j) {

                var fromAddress = message['x']['inputs'][j]["prev_out"]["addr"];

                /*
                var fromValue = 0;
                try {
                    fromValue = message['x']['inputs'][j]['prev_out']['value'];
                }
                catch (e) {
                    ;; //console.log(e) 
                }
                */

                if (fromAddress != null) {
                    graph.addNode(fromAddress, 
                                  {url : 'https://blockchain.info/tx/' 
                                          + fromAddress,
                                   id : fromAddress 
                            });
                    ++i;
                }

                if ((fromAddress != null) && (txAddress != null)) {
                    graph.addLink(fromAddress, txAddress);
                }

            }

            for (var j = 0; j < message['x']['out'].length; ++j) {

                var toAddress = message['x']['out'][j]['addr'];

                /*
                var toValue = 0;
                try {
                    toValue = message['x']['out'][j]['value'];
                }
                catch (e) {
                    ;; //console.log(e) 
                }
                */

                if (toAddress != null) {
                    graph.addNode(toAddress, 
                                  {url : 'https://blockchain.info/tx/' 
                                         + toAddress
                            });
                    ++i;
                }

                if ((txAddress != null) && (toAddress != null)) {
                    graph.addLink(txAddress, toAddress, 
                                  {url : 'https://blockchain.info/tx/'
                                         + txAddress
                            });
                }

            }

            graph.endUpdate();

            if (i >= maxNodes) {
                setTimeout(function() {
                    beginRemoveNodesLoop(graph);
                    //domLabels = generateDOMLabels(graph);
                }, 10000);
            }
        }
    }
    catch (err) {
        if (err != null) { 
            ; //console.log(err.message); 
        }
    }
    }).fail(function(e) {
        //console.log(e);
    });

    blockchainWebSocket.send({ 'op': 'ping' }).done(function() {
        // message send
        //console.log("blockchain websocket ping sent. failures will be logged.");
    }).fail(function(e) {
        // error sending
        console.log("blockchain websocket ping failed!");
    });

    blockchainWebSocket.send({ 'op': 'unconfirmed_sub' }).done(function() {
        // message send
        //console.log("blockchain subscription requested. failures will be logged.");
    }).fail(function(e) {
        // error sending
        console.log("blockchain subscription failed!");
    });

}
