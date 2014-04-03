import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');
import punctuations = require('./punctuations');


/**
 * Base class for RxFlow dataflow elements.
 */
class DataflowElement extends punctuations.PunctuationHandlerMixin {

    private inputs: Array<InputPort<any>> = [];
    private outputs: Array<OutputPort<any>> = [];

    registerInput(input: InputPort<any>) {
        this.inputs.push(input);
    }

    registerOutput(output: OutputPort<any>) {
        this.outputs.push(output);
    }

    handleEndOfRound() {
        this.flush();
    }

    getNumInputs(): number {
        return this.inputs.length;
    }

    sendPunctuationDownstream(punc: punctuations.Punctuation) {
        this.outputs.forEach(output => output.onNext(punc));
    }

    flush() {
        // This space intentionally left empty
    }

    invalidate() {
        // This space intentionally left empty
    }
}

export = DataflowElement;
