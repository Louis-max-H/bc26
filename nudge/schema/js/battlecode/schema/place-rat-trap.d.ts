import * as flatbuffers from 'flatbuffers';
/**
 * Place a rat trap at location
 */
export declare class PlaceRatTrap {
    bb: flatbuffers.ByteBuffer | null;
    bb_pos: number;
    __init(i: number, bb: flatbuffers.ByteBuffer): PlaceRatTrap;
    loc(): number;
    team(): number;
    static sizeOf(): number;
    static createPlaceRatTrap(builder: flatbuffers.Builder, loc: number, team: number): flatbuffers.Offset;
}
