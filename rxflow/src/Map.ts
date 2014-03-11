import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');

class Map<T, R> extends DataflowElement {

    private mapFunc: (T) => R;

    constructor(mapFunc: (T) => R) {
        super();
        this.mapFunc = mapFunc;
    }

    output = new OutputPort(this);
    input = new InputPort(x => this.output.onNext(this.mapFunc(x)), this);
}

export = Map;
