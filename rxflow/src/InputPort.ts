import DataflowElement = require('./DataflowElement');
import OutputPort = require('./OutputPort');
import punctuations = require('./punctuations');


class InputPort<T> extends punctuations.PunctuationHandlerMixin {

    private producers: Array<OutputPort<T>> = [];
    private onNextValue: (T) => void;
    private elem: DataflowElement;

    constructor(onNextValue: (T) => void, elem: DataflowElement = null) {
        super();
        this.onNextValue = onNextValue;
        this.elem = elem;
        if (elem != null) {
            elem.registerInput(this);
        }
    }

    addProducer(producer: OutputPort<T>) {
        this.producers.push(producer);
    }

    onNext(val: any): void {
        if (val instanceof punctuations.Punctuation) {
            this.handlePunctuation(<punctuations.Punctuation> val, null);
        } else {
            this.onNextValue(val);
        }
    }

    getNumInputs(): number {
        return this.producers.length;
    }

    sendPunctuationDownstream(punc: punctuations.Punctuation) {
        if (this.elem != null) {
            this.elem.handlePunctuation(punc, this);
        }
    }
}

export = InputPort;
