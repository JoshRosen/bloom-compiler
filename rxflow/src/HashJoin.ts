import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');


class HashJoin extends DataflowElement {

    leftInput: InputPort<any>;
    rightInput: InputPort<any>;
    output = new OutputPort(this);

    private buildInput: InputPort<any>;
    private probeInput: InputPort<any>;

    private hashTable = {};

    private buildKeyFunc;
    private probeKeyFunc;
    private resultOrderingFunction;

    constructor(leftKeyFunc, rightKeyFunc, buildInput) {
        super();
        if (buildInput === 'left') {
            this.leftInput = new InputPort(x => this.handleBuildInput(x), this);
            this.rightInput = new InputPort(x => this.handleProbeInput(x), this);
            this.buildInput = this.leftInput;
            this.probeInput = this.rightInput;
            this.buildKeyFunc = leftKeyFunc;
            this.probeKeyFunc = rightKeyFunc;
            this.resultOrderingFunction = function (b, p) {  return [b, p]; };
        } else if (buildInput === 'right') {
            this.rightInput = new InputPort(x => this.handleBuildInput(x), this);
            this.leftInput = new InputPort(x => this.handleProbeInput(x), this);
            this.buildInput = this.rightInput;
            this.probeInput = this.leftInput;
            this.buildKeyFunc = rightKeyFunc;
            this.probeKeyFunc = leftKeyFunc;
            this.resultOrderingFunction = function (b, p) {  return [p, b]; };
        } else {
            throw new Error('buildInput should be \'left\' or \'right\', not \'' + buildInput + '\'');
        }
    }


    private handleProbeInput(p) {
        var key = this.probeKeyFunc(p);
        if (key in this.hashTable) {
            var matches = this.hashTable[key];
            matches.forEach(b => this.output.onNext(this.resultOrderingFunction(b, p)));
        }
    }

    private handleBuildInput(b) {
        var key = this.buildKeyFunc(b);
        if (!(key in this.hashTable)) {
            this.hashTable[key] = [b];
        } else {
            this.hashTable[key].push(b);
        }
    }

}

export = HashJoin;
