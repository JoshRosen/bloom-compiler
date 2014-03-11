import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');
import punctuations = require('./punctuations');


/**
 * Base class for RxFlow dataflow elements.
 */
class DataflowElement {

    private inputs: Array<InputPort<any>> = [];
    private outputs: Array<OutputPort<any>> = [];
    private eorCount = 0;

    registerInput(input: InputPort<any>) {
        this.inputs.push(input);
    }

    registerOutput(output: OutputPort<any>) {
        this.outputs.push(output);
    }

    handlePunctuation(punc, port: InputPort<any>) {
        if (punc === punctuations.END_OF_ROUND) {
            this.eorCount += 1;
            if (this.eorCount === this.inputs.length) {
                this.flush();
                this.outputs.forEach(output => output.onNext(punctuations.END_OF_ROUND));
                this.eorCount = 0;
            }
        }
    }

    flush() {
        // This space intentionally left empty
    }

    invalidate() {
        // This space intentionally left empty
    }
}

export = DataflowElement;
