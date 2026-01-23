import * as flatbuffers from 'flatbuffers';
/**
 * Indicate that a dirt wall is destroyed
 */
export declare class BreakDirt {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): BreakDirt;
    loc(): number;
    static sizeOf(): number;
    static createBreakDirt(builder: flatbuffers.Builder, loc: number): flatbuffers.Offset;
}
