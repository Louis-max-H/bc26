import * as flatbuffers from 'flatbuffers';
/**
 * Create a Rat King at the specified location
 */
export declare class CreateRatKing {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): CreateRatKing;
    /**
     * center square of the rat king
     * (all rats in the 3x3 square centered here get converted to the king)
     */
    center(): number;
    static sizeOf(): number;
    static createCreateRatKing(builder: flatbuffers.Builder, center: number): flatbuffers.Offset;
}
