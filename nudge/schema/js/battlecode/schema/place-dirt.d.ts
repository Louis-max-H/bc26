import * as flatbuffers from 'flatbuffers';
/**
 * Indicate that a dirt wall is placed
 */
export declare class PlaceDirt {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): PlaceDirt;
    loc(): number;
    static sizeOf(): number;
    static createPlaceDirt(builder: flatbuffers.Builder, loc: number): flatbuffers.Offset;
}
