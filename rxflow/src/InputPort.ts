import DataflowElement = require('./DataflowElement');
import OutputPort = require('./OutputPort');


class InputPort<T> {

    producers: Array<OutputPort<T>> = [];

    onNext: (T) => void;

    onCompleted() {
        // Intentionally left blank
    }

    constructor(onNext: (T) => void) {
        this.onNext = onNext;
    }

}

export = InputPort;
