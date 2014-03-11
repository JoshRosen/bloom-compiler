import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');


class OutputPort<T> {

    consumers: Array<InputPort<T>> = [];

    onNext(val: T) {
        this.consumers.forEach(consumer => consumer.onNext(val));
    }

    subscribe(inputPort: InputPort<T>) {
        this.consumers.push(inputPort);
        inputPort.producers.push(this);
    }

}

export = OutputPort;
