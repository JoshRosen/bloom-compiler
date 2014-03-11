import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');

class OutputPort<T> {

    private consumers: Array<InputPort<T>> = [];

    constructor(elem: DataflowElement) {
        if (elem != null) {
            elem.registerOutput(this);
        }
    }

    onNext(val: T) {
        this.consumers.forEach(consumer => consumer.onNext(val));
    }

    subscribe(inputPort: InputPort<T>) {
        this.consumers.push(inputPort);
        inputPort.addProducer(this);
    }

}

export = OutputPort;
