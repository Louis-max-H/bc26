import * as flatbuffers from 'flatbuffers';
/**
 * Cat jump from start to end
 */
export declare class CatPounce {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): CatPounce;
    startLoc(): number;
    endLoc(): number;
    static sizeOf(): number;
    static createCatPounce(builder: flatbuffers.Builder, startLoc: number, endLoc: number): flatbuffers.Offset;
}
