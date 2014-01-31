var Rx = require('rx');
var rxflow = require('rxflow');


function Bloom () {
    var linkDelta = new Rx.Subject();
    var pathDelta = new Rx.Subject();

    this.linkIn = new Rx.Subject();
    this.linkIn.subscribe(linkDelta);

    this.pathOut = new Rx.Subject();

    // For now; I don't have tables.  Later, I'm going to add
    // a Table with observable change streams to RxFlow.

    linkDelta.select(function(l) { return [l[0], l[1], l[1], l[2]]; }).subscribe(pathDelta);

    var linkDeltaJoinPath = new rxflow.HashJoin(
        function(l) { return l[1]; },
        function(p) { return p[0]; },
        "right"
    );
    linkDelta.subscribe(linkDeltaJoinPath.leftInput);
    pathDelta.subscribe(linkDeltaJoinPath.rightInput);


    var pathDeltaJoinLink = new rxflow.HashJoin(
        function(l) { return l[1]; },
        function(p) { return p[0]; },
        "left"
    );
    linkDelta.subscribe(pathDeltaJoinLink.leftInput);
    pathDelta.subscribe(pathDeltaJoinLink.rightInput);

    linkDeltaJoinPath.output.merge(pathDeltaJoinLink.output).map(function(x) {
        var l = x[0];
        var p = x[1];
        return [l[0], p[1], l[1], l[2] + p[3]];
    }).subscribe(pathDelta);

    pathDelta.distinct().subscribe(this.pathOut);
}

var bloom = new Bloom();

bloom.pathOut.subscribe(function(x) { console.log(x)});
bloom.linkIn.onNext(['a', 'b', 1]);
bloom.linkIn.onNext(['a', 'b', 4]);
bloom.linkIn.onNext(['b', 'c', 1]);
bloom.linkIn.onNext(['c', 'd', 1]);
bloom.linkIn.onNext(['d', 'e', 1]);