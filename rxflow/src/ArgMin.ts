import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');
import Aggregate = require('./Aggregate');


class ArgMin<T> extends DataflowElement {

    private aggregate: Aggregate<T>;
    input: InputPort<T>;
    output = new OutputPort(this);

    constructor(keyFunction, orderingFields, orderingFunction) {
        super();
        var aggregateFunction = function () {
            var value = null;
            this.getValue = function() { return value; };
            this.next = function(x) {
                if (value === null || orderingFunction(orderingFields(x), orderingFields(value))) {
                    value = x;
                }
            };
        };
        this.aggregate = new Aggregate(keyFunction, [aggregateFunction]);
        this.input = this.aggregate.input;
        var outputProjector = new InputPort(x => this.output.onNext(x[1]), null);
        this.aggregate.output.subscribe(outputProjector);
    }

    flush() {
        this.aggregate.flush();
    }

    invalidate() {
        this.aggregate.invalidate();
    }
}

export = ArgMin;
