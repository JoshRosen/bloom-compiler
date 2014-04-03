
export class Punctuation {
    // This space intentionally left blank
}

/**
 * Indicates the end of a batch of input.  This is roughly equivalent to "end-of-stream", except
 * it doesn't wipe out all operator state.
 */
export class EndOfRound extends Punctuation {
    round: number;
    constructor(round: number) {
        super();
        this.round = round;
    }
}

/**
 * Mixin trait that implements the state machine governing punctuation propagation.
 */
export class PunctuationHandlerMixin {
    private currentRound = 0;
    private eorCount = 0;

    getNumInputs(): number {
        throw new Error('getNumInputs() is abstract and must be implemented by subclasses');
    }

    handleEndOfRound(): void {
        this.flush();
    }

    flush(): void {
        // This space intentionally left blank
    }

    sendPunctuationDownstream(punc: Punctuation): void {
        throw new Error('sendPunctuationDownstream() is abstract and must be implemented by subclasses');
    }

    handlePunctuation(punc: Punctuation, source: any): void {
        var numInputs = this.getNumInputs();
        if (punc instanceof EndOfRound && (<EndOfRound> punc).round === this.currentRound) {
            this.eorCount += 1;
            if (this.eorCount === numInputs || numInputs === 0) {
                this.eorCount = 0;
                this.currentRound += 1;
                this.handleEndOfRound();
                this.sendPunctuationDownstream(punc);
            }
        }
    }
}
