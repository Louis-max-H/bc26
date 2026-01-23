import * as flatbuffers from 'flatbuffers';
/**
 * Happens when a cat tries to pounce on a trap
 */
export declare class TriggerCatTrap {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): TriggerCatTrap;
    loc(): number;
    team(): number;
    static sizeOf(): number;
    static createTriggerCatTrap(builder: flatbuffers.Builder, loc: number, team: number): flatbuffers.Offset;
}
