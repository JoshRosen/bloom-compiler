/// <reference path="../typings/rx.js/rx.d.ts" />

import Rx = require('rx');
import Buffer = require('./Buffer');
import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');
import DataflowElement = require('./DataflowElement');
import punctuations = require('./punctuations');

class ObservableScanner<T> extends DataflowElement {

    private buffer = new Buffer();
    output: OutputPort<T> = this.buffer.output;

    constructor(observable: Rx.Observable<T>) {
        super();
        observable.subscribe(Rx.Observer.create(x => this.buffer.input.onNext(x)));
    }

    flush() {
        return this.buffer.flush();
    }

    endRound(round: number) {
        this.buffer.input.onNext(new punctuations.EndOfRound(round));
    }
}

export = ObservableScanner;
