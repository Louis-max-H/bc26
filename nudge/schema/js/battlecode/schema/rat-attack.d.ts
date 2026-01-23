import * as flatbuffers from 'flatbuffers';
/**
 * Rat attacks another rat
 */
export declare class RatAttack {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): RatAttack;
    /**
     * Id of the attacked rat
     */
    id(): number;
    static sizeOf(): number;
    static createRatAttack(builder: flatbuffers.Builder, id: number): flatbuffers.Offset;
}
