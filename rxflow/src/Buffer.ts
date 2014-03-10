import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');

class Buffer<T> extends DataflowElement {

    private buffer: Array<T> = [];

    input = new InputPort<T>(x => this.buffer.push(x));
    output = new OutputPort<T>();


    invalidate() {
        this.buffer = [];
    }

    isEmpty(): boolean {
        return this.buffer.length === 0;
    }

    flush() {
        var oldBuffer = this.buffer;
        this.buffer = [];
        oldBuffer.forEach(x => this.output.onNext(x));
        return oldBuffer.length;
    }
}

export = Buffer;
