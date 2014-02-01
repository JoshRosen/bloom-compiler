var Rx = require('rx');
var Ix = require('ix');

function Bloom () {
    var outerThis = this;

    this.linkIn = new Rx.Subject();
    this.linkIn.subscribe(function(x) {
        linkDelta = linkDelta.union(Ix.Enumerable.return(x));
        handleDeltas();
    });
    this.linkOut = new Rx.Subject();

    this.pathIn = new Rx.Subject();
    this.pathIn.subscribe(function(x) {
        pathDelta = pathDelta.union(Ix.Enumerable.return(x));
        handleDeltas();
    });
    this.pathOut = new Rx.Subject();


    var linkTable = Ix.Enumerable.empty();
    var pathTable = Ix.Enumerable.empty();

    var linkDelta = Ix.Enumerable.empty();
    var pathDelta = Ix.Enumerable.empty();

    function rule0(link) {
        return link.map(function(l) { return [l[0], l[1], l[1], l[2]]; });
    }

    function rule1(link, path) {
        return link.join(path,
            function(l) { return l[1]; },
            function(p) { return p[0]; },
            function(l, p) {return [l[0], p[1], l[1], l[2] + p[3]];});
    }

    function handleDeltas()  {
        while (!linkDelta.isEmpty() || !pathDelta.isEmpty()) {
            if (!linkDelta.isEmpty()) {
                var pathDeltaNew = Ix.Enumerable.empty();
                var linkDeltaNew = Ix.Enumerable.empty();
                pathDeltaNew = rule0(linkDelta).union(pathDeltaNew);
                pathDeltaNew = rule1(linkDelta, pathTable).union(pathDeltaNew);
                linkTable = linkTable.union(linkDelta);
                linkDelta.forEach(function(x) { outerThis.linkOut.onNext(x); });
                pathDelta = pathDeltaNew.except(pathTable);
                linkDelta = linkDeltaNew.except(linkTable);
            }
            if (!pathDelta.isEmpty()) {
                var pathDeltaNew = Ix.Enumerable.empty();
                pathDeltaNew = rule1(linkTable, pathDelta).union(pathDeltaNew);
                pathTable = pathTable.union(pathDelta);
                pathDelta.forEach(function(x) { outerThis.pathOut.onNext(x); });
                pathDelta = pathDeltaNew.except(pathTable);
            }
        }
    }
}

var bloom = new Bloom();

bloom.pathOut.subscribe(function(x) { console.log(x)});
bloom.linkIn.onNext(['a', 'b', 1]);
bloom.linkIn.onNext(['a', 'b', 4]);
bloom.linkIn.onNext(['b', 'c', 1]);
bloom.linkIn.onNext(['c', 'd', 1]);
bloom.linkIn.onNext(['d', 'e', 1]);