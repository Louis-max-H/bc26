import * as flatbuffers from 'flatbuffers';
/**
 * Happens when a rat tries to pick up cheese on a trap
 */
export declare class TriggerRatTrap {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): TriggerRatTrap;
    loc(): number;
    team(): number;
    static sizeOf(): number;
    static createTriggerRatTrap(builder: flatbuffers.Builder, loc: number, team: number): flatbuffers.Offset;
}
