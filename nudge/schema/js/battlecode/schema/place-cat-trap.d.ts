import * as flatbuffers from 'flatbuffers';
/**
 * Place a cat trap at location
 */
export declare class PlaceCatTrap {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): PlaceCatTrap;
    loc(): number;
    team(): number;
    static sizeOf(): number;
    static createPlaceCatTrap(builder: flatbuffers.Builder, loc: number, team: number): flatbuffers.Offset;
}
