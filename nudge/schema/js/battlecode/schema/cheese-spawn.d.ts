import * as flatbuffers from 'flatbuffers';
/**
 * Spawn a cheese at a location
 */
export declare class CheeseSpawn {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): CheeseSpawn;
    loc(): number;
    amount(): number;
    static sizeOf(): number;
    static createCheeseSpawn(builder: flatbuffers.Builder, loc: number, amount: number): flatbuffers.Offset;
}
