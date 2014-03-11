import DataflowElement = require('./DataflowElement');
import OutputPort = require('./OutputPort');
import punctuations = require('./punctuations');


class InputPort<T> {

    private producers: Array<OutputPort<T>> = [];
    private eorCount = 0;
    private onNextValue: (T) => void;
    private elem: DataflowElement;

    constructor(onNextValue: (T) => void, elem: DataflowElement = null) {
        this.onNextValue = onNextValue;
        this.elem = elem;
        if (elem != null) {
            elem.registerInput(this);
        }
    }

    addProducer(producer: OutputPort<T>) {
        this.producers.push(producer);
    }

    onNext(val: T): void {
        if (val === punctuations.END_OF_ROUND) {
            this.eorCount += 1;
            if (this.eorCount === this.producers.length) {
                if (this.elem != null) {
                    this.elem.handlePunctuation(punctuations.END_OF_ROUND, this);
                }
                this.eorCount = 0;
            }
        } else {
            this.onNextValue(val);
        }
    }

}

export = InputPort;
