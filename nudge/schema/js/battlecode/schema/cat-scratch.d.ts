import * as flatbuffers from 'flatbuffers';
/**
 * Cat scratch at location
 */
export declare class CatScratch {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): CatScratch;
    loc(): number;
    static sizeOf(): number;
    static createCatScratch(builder: flatbuffers.Builder, loc: number): flatbuffers.Offset;
}
