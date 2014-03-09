/// <reference path="../typings/rx.js/rx.d.ts" />
/// <reference path="./DataflowElement.ts" />

import Rx = require('rx');


interface AggregationFunction<T, A> {
    /**
     * Returns the current value of the aggregate.
     */
    getValue(): A;

    /**
     * Update the aggregate with a new value.
     */
    next(val: T): void;
}

/**
 * Performs GROUP BY aggregation.
 */
class Aggregate<T> implements DataflowElement {

    private aggregators: Array<Array<AggregationFunction<T, any>>> = [];
    private groupKeys = [];
    private keyToArrayIndex = {};
    private nextArrayIndex: number = 0;
    private keyFunction: (T) => any;
    private aggregates;

    /**
     * Create a new Aggregate.
     *
     * @param keyFunction
     *      A function that extracts the key from each record.
     *      The key must be a hashable Javascript object.
     * @param aggregates
     *      A list of aggregation function classes.
     */
    constructor(keyFunction: (T) => any, aggregates) {
        this.keyFunction = keyFunction;
        this.aggregates = aggregates;
    }

    createAggregators() {
        return this.aggregates.map((Cls) => new Cls());
    }

    updateAggs(x: T) {
        var key = this.keyFunction(x);
        var idx = this.keyToArrayIndex[key];
        if (idx === undefined) {
            this.keyToArrayIndex[key] = idx = this.nextArrayIndex;
            this.nextArrayIndex += 1;
            this.aggregators.push(this.createAggregators());
            this.groupKeys.push(key);
        }
        this.aggregators[idx].forEach((agg) => agg.next(x));
    }

    /**
     * An input stream of elements to be aggregated.
     */
    input = Rx.Observer.create<T>((x: T) => this.updateAggs(x));

    output = new Rx.Subject();

    /**
     * Return a stream of groups and values as an Rx observable.
     */
    getCurrentValues() {
        return Rx.Observable.create((observer: any) => {
            for (var i = 0; i < this.aggregators.length; ++i) {
                observer.onNext([this.groupKeys[i]].concat(this.aggregators[i].map((agg) => agg.getValue())));
            }
        });
    }

    flush() {
        this.getCurrentValues().subscribe(() => this.output);
    }

    /**
     * Reset this element by resetting aggregates to their initial values
     * and clearing all groups.
     */
    invalidate() {
        this.aggregators = [];
        this.groupKeys = [];
        this.keyToArrayIndex = {};
        this.nextArrayIndex = 0;
    }
}

export = Aggregate;
