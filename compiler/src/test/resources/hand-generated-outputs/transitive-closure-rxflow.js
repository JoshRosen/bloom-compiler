function Bloom () {
    var rx = require('rx');
    var rxflow = require('rxflow');

    var tables = {
        "path": new rxflow.Table(3) /* table path, [from: string, to: string, nxt: string, cost: int] */
    };

    var inputs = {
        "link": new rx.Subject() /* input link, [from: string, to: string, cost: int] */
    };
    this.link = inputs["link"];

    var outputs = {
        "pathOut": new rx.Subject() /* output pathOut, [from: string, to: string, nxt: string, cost: int] */
    };
    this.pathOut = outputs["pathOut"];

    var elements = {
        6: new rxflow.HashJoin(
            function(x) { return x[1]; /* link.to */ },
            function(x) { return x[0]; /* path.from */ },
            "right"
        ),
        5: new rxflow.HashJoin(
            function(x) { return x[1]; /* link.to */ },
            function(x) { return x[0]; /* path.from */ },
            "left"
        ),
        0: new rxflow.Map(
            function(x) { return [x[0], x[1], x[1], x[2]]; /* [l.from, l.to, l.to, l.cost] */ }
        ),
        2: new rxflow.ObservableScanner(inputs["link"]),
        7: new rxflow.Map(
            function(x) { return [x[0][0], x[1][1], x[0][1], x[0][2] + x[1][3]]; /* [l.from, p.to, l.to, l.cost + p.cost] */ }
        ),
        4: new rxflow.TableScanner(tables["path"])
    };

    var invalidationLookupTable = { "link": [2], "path": [7, 5, 0, 6] };

    var rescanLookupTable = { "path": [2, 4] };

    // Wiring, in a roughly topological order
    // Scanner outputs:
    elements[2].output.subscribe(elements[5].leftInput);
    elements[2].output.subscribe(elements[6].leftInput);
    elements[2].output.subscribe(elements[0].input);
    elements[4].output.subscribe(elements[5].rightInput);
    elements[4].output.subscribe(elements[6].rightInput);
    elements[4].output.subscribe(outputs["pathOut"]);

    // Join outputs:
    elements[5].output.subscribe(elements[7].input);
    elements[6].output.subscribe(elements[7].input);

    // Table insertions:
    elements[0].output.subscribe(tables["path"].insert);  // Link -> Path map
    elements[7].output.subscribe(tables["path"].insert);  // Join projection

    function tickInternal() {
        var tuplesFlushed = 0;
        tuplesFlushed += elements[2].flush();
        tuplesFlushed += elements[4].flush();
        return tuplesFlushed;
    }

    this.tick = function() {
        var atFixpoint = false;
        while (!atFixpoint) {
            atFixpoint = tickInternal() === 0;
        }
    }
}

var bloom = new Bloom();

bloom.pathOut.subscribe(function(x) { console.log(x)});
bloom.link.onNext(['a', 'b', 1]);
bloom.link.onNext(['a', 'b', 4]);
bloom.link.onNext(['b', 'c', 1]);
bloom.link.onNext(['c', 'd', 1]);
bloom.link.onNext(['d', 'e', 1]);
bloom.tick();