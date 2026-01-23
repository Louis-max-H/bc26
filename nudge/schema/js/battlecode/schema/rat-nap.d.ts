import * as flatbuffers from 'flatbuffers';
/**
 * Kidnap a rat
 */
export declare class RatNap {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): RatNap;
    /**
     * Id of the napping victim
     */
    id(): number;
    static sizeOf(): number;
    static createRatNap(builder: flatbuffers.Builder, id: number): flatbuffers.Offset;
}
