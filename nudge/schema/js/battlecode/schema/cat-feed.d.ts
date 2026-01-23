import * as flatbuffers from 'flatbuffers';
/**
 * Cat feeds on a rat
 */
export declare class CatFeed {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): CatFeed;
    /**
     * the ID of the eaten rat
     */
    id(): number;
    static sizeOf(): number;
    static createCatFeed(builder: flatbuffers.Builder, id: number): flatbuffers.Offset;
}
