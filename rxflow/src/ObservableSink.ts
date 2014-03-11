/// <reference path="../typings/rx.js/rx.d.ts" />

import Rx = require('rx');
import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');

class ObservableSink<T> extends DataflowElement {

    output = new Rx.Subject<T>();

    input = new InputPort(x => this.output.onNext(x), this);

}

export = ObservableSink;
