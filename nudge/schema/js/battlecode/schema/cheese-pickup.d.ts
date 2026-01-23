import * as flatbuffers from 'flatbuffers';
/**
 * Indicate that a cheese has been picked up from the map by a rat
 */
export declare class CheesePickup {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): CheesePickup;
    loc(): number;
    static sizeOf(): number;
    static createCheesePickup(builder: flatbuffers.Builder, loc: number): flatbuffers.Offset;
}
