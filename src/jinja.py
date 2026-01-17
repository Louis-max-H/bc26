import argparse
from jinja2 import Environment, FileSystemLoader, nodes
from jinja2.ext import Extension
from collections import defaultdict
import os
import re
from pathlib import Path

import math
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap
import numpy as np

############################### Variables ###############################

directions = ["WEST", "NORTHWEST", "SOUTHWEST", "SOUTH", "SOUTHEAST", "EAST", "NORTHEAST", "NORTH", "CENTER"]
directionsShort = ["W", "NW", "SW", "S", "SE", "E", "NE", "N", "C"]
dirsOrds = {"NORTH":0, "NORTHEAST":1, "EAST":2, "SOUTHEAST":3, "SOUTH":4, "SOUTHWEST":5, "WEST":6, "NORTHWEST":7, "CENTER":8}
dirsOrdsOpposite = {"NORTH":4, "NORTHEAST":5, "EAST":6, "SOUTHEAST":7, "SOUTH":0, "SOUTHWEST":1, "WEST":2, "NORTHWEST":3, "CENTER":8}
ordsDirsOpposite = {0:"NORTH", 1:"NORTHEAST", 2:"EAST", 3:"SOUTHEAST", 4:"SOUTH", 5:"SOUTHWEST", 6:"WEST", 7:"NORTHWEST", 8:"CENTER"}

directionsLong = [
    "Direction.WEST",
    "Direction.NORTHWEST",
    "Direction.SOUTHWEST",
    "Direction.SOUTH",
    "Direction.SOUTHEAST",
    "Direction.EAST",
    "Direction.NORTHEAST",
    "Direction.NORTH",
    "Direction.CENTER"]
directionsWhitoutCenter = ["WEST", "NORTHWEST", "SOUTHWEST", "SOUTH", "SOUTHEAST", "EAST", "NORTHEAST", "NORTH"]

anglesToDirections = {
    180 : "WEST",
    135 : "NORTHWEST",
    225 : "SOUTHWEST",
    270 : "SOUTH",
    315 : "SOUTHEAST",
    0 : "EAST",
    45 : "NORTHEAST",
    90 : "NORTH",
}

directionsToAngle = {
    "WEST":  180,
    "NORTHWEST": 135,
    "SOUTHWEST": 225,
    "SOUTH":  270,
    "SOUTHEAST": 315,
    "EAST":  0,
    "NORTHEAST": 45,
    "NORTH":  90,
    "CENTER":  0
}
unitsVisionRadius = {"BABY_RAT":20, "RAT_KING":25, "CAT":30}
unitsVisionAngle = {"BABY_RAT":90, "RAT_KING":360, "CAT":120}
unitsOrdinal = {"BABY_RAT":0, "RAT_KING":1, "CAT":2}
directionsOrdinal = {"WEST": 0, "NORTHWEST": 1, "SOUTHWEST": 2, "SOUTH": 3, "SOUTHEAST": 4, "EAST": 5, "NORTHEAST": 6, "NORTH": 7, "CENTER": 8}

dirsDelta = {
    "NORTH" :  (0, 1),
    "NORTHEAST" : (1,1),
    "EAST" : (1,0),
    "SOUTHEAST" : (1,-1),
    "SOUTH" : (0,-1),
    "SOUTHWEST" : (-1,-1),
    "WEST" : (-1,0),
    "NORTHWEST" : (-1,1),
    "CENTER" : (0,0)
}

dirsShift60xy = {}
for dir, coo in dirsDelta.items():
    dirsShift60xy[dir] = coo[0] + 60 * coo[1]

def rotate(direction, indicator, amount):
    if indicator.upper() == "RIGHT":
        shift = 45
    elif indicator.upper() == "LEFT":
        shift = -45
    else:
        return "INVALID_INDICATOR"

    return anglesToDirections[(directionsToAngle[direction] + shift * amount) % 360]


############################### Utils ###############################

def sanitizeOperation(string):
    return string.replace("+ -", "- ").replace("+ 0", "")

def shiftCells(cells, direction):
    dx, dy = dirsDelta[direction]
    return [(x + dx, y + dy) for x, y in cells]

def reverseRange(n):
    return reversed(list(range(n)))

def encodeXY(cell, gap):
    x, y = cell
    return (x+gap) + (y+gap)*(60+gap)

def encodeShift(cell, gap): # Same has encode, but we dont have to shift to place the cell [0, 0] to [gap, gap]
    x, y = cell
    shift = x + y*(60 + gap)

    # Since the value on witch we are calling the shift is an int, dont need to use overflow
    # if shift < 0:
    #    return 2**16 - shift

    return shift

def encodeXYString(gap):
    # return (x+gap) + (y+gap)*(60+gap)
    # return x + y*(60+gap) + gap + gap*(60+gap)
    if gap != 0:
        constShift = gap + gap*(60+gap)
        return f"x + y*{(60 + gap)} + {constShift}"
    return f"x + y*60"

def encodeXYLoc(gap):
    if gap != 0:
        return f"loc.x + loc.y*{(60 + gap)} + {gap + gap*(60+gap)}"
    return f"loc.x + loc.y*60"



def getAngle(x, y):
    angle = math.atan2(y, x) # [-pi, pi]
    angle = angle * 180 / math.pi # [-180, 180]
    if angle < 0:
        angle += 360 # [0, 360]
    return angle

def intToChar(valeur):
    return "\\u"+hex(valeur)[2:].zfill(4)


def isAngleInBounds(angle_min, angle_max, angle):
    """
    Returns True if the angle is within [angle_min, angle_max] in degrees, handling wrap-around and negative/over-360 angles.
    All input angles can be any real number. Intervals with angle_max < angle_min are considered wrapping across 0.
    """

    # Normalize all angles to [0, 360)
    norm = lambda a: a % 360
    amin = norm(angle_min)
    amax = norm(angle_max)
    a = norm(angle)

    if amin <= amax:
        # Simple non-wrapping interval
        return amin <= a <= amax
    else:
        # Wrapping interval (e.g., 300 to 60 means [300, 360) U [0, 60])
        return a >= amin or a <= amax

def visionCells(unit, direction):
    if not unit in unitsVisionRadius.keys():
        print(f"Unit {unit} is unknow. Should be in {list(unitsVisionRadius.keys())}")
        return []

    if not direction in directionsToAngle.keys():
        print(f"Direction {direction} is unknow. Should be in {list(directionsToAngle.keys())}")
        return []

    R = unitsVisionRadius[unit]
    A = unitsVisionAngle[unit]
    cappedR = int(R**0.5 + 1)
    angleDirection = directionsToAngle[direction]

    cells = []
    for x in range(-cappedR, cappedR + 1):
        for y in range(-cappedR, cappedR + 1):

            if (x, y) == (0, 0):
                cells.append((x, y))
                continue

            dist = x**2 + y**2
            if (dist > R):
                continue

            if A < 360 and not isAngleInBounds(
                angleDirection - A/2,
                angleDirection + A/2,
                getAngle(x, y)
            ):
                continue

            cells.append((x, y))
    return cells

def visionCellsXY(unit, direction, gap):
    return [encodeShift(cell, gap) for cell in visionCells(unit, direction)]


############################### Generating functions ###############################

def genVisionCell(unit, direction, movement):
    cells = shiftCells(visionCells(unit, direction), movement)
    return sanitizeOperation(
        "new MapLocation[]{" + ", ".join(
            [f"new MapLocation(x + {x}, y + {y})" for x, y in cells]
        ) + "}"
    )

def genScoreInView(unit, direction, movement, gap=8):
    result = ""
    for cell in shiftCells(visionCells(unit, direction), movement):
        result += f" + scores[xy + { encodeShift(cell, gap) }]"

    return sanitizeOperation(result)

def genMemoryCharArray(defaultValue, gapValue, gap):
    # Convert gapValue to hex \u0000
    gapValue = "\\u"+hex(gapValue)[2:].zfill(4)
    defaultValue = "\\u"+hex(defaultValue)[2:].zfill(4)

    array = [gapValue for _ in range((60 + gap*2) * (60 + gap*2))]
    for x in range(60):
        for y in range(60):
            array[encodeXY((x, y), gap)] = defaultValue
    return  '"' + "".join(array) + '".toCharArray()'


############################### Jinja toolchain ###############################

def extract_destination(template_path):
    """Extract the destination path from a .jinja2 file."""
    with open(template_path, 'r', encoding='utf-8') as f:
        for line in f:
            # Look for line starting with # Destination:
            match = re.match(r'//\s*Destination:\s*(.+)', line)
            if match:
                return match.group(1).strip()
    return None

def process_template(template_path, base_dir, is_prod):
    """Process a single .jinja2 template file."""
    print(f"Processing: {template_path}")

    # Extract destination from template
    destination = extract_destination(template_path)
    if not destination:
        print(f"  ⚠️  Warning: No '# Destination:' line found in {template_path}")
        return None

    # Resolve destination path relative to base directory
    if os.path.isabs(destination):
        output_path = destination
    else:
        output_path = os.path.join(base_dir, destination)

    # Normalize path
    output_path = os.path.normpath(output_path)

    print(f"  Destination: {output_path}")

    # Extract class name from output path (filename without extension)
    output_filename = os.path.basename(output_path)
    class_name = os.path.splitext(output_filename)[0]

    # Setup Jinja2 environment
    template_dir = os.path.dirname(template_path)
    env = Environment(loader=FileSystemLoader(searchpath=template_dir), trim_blocks=True)

    # Add global utility functions
    env.globals['visionCells'] = visionCells

    # Get template name (relative to template_dir)
    template_name = os.path.basename(template_path)
    template = env.get_template(template_name)

    # Render template
    rendered_content = template.render(
        ratsUnits=["BABY_RAT", "RAT_KING"],
        directions=directions,
        dirsDelta=dirsDelta,
        unitsOrdinal=unitsOrdinal,
        reverseRange=reverseRange,
        directionsLong=directionsLong,
        directionsShort=directionsShort,
        anglesToDirections=anglesToDirections,
        unitsVisionAngle=unitsVisionAngle,
        unitsVisionRadius=unitsVisionRadius,
        directionsOrdinal=directionsOrdinal,
        directionsWhitoutCenter=directionsWhitoutCenter,
        encodeXY=encodeXY,
        encodeShift=encodeShift,
        dirsShift60xy=dirsShift60xy,
        rotate=rotate,
        encodeXYLoc=encodeXYLoc,
        encodeXYString=encodeXYString,
        genVisionCell=genVisionCell,
        visionCellsXY=visionCellsXY,
        genScoreInView=genScoreInView,
        dirsOrds=dirsOrds,
        intToChar=intToChar,
        dirsOrdsOpposite=dirsOrdsOpposite,
        ordsDirsOpposite=ordsDirsOpposite,
        genMemoryCharArray=genMemoryCharArray,
        prod=is_prod,
        className=class_name
    )

    # Calculate statistics
    if is_prod:
        # Add header comment for generated files
        header = """// #########################################################
// !!!!!!!!!! This file is generated by jinja.py !!!!!!!!!!!
// !!!! Dont edit it manually, check in template folder !!!!
// #########################################################

"""
        final_content = sanitizeOperation(header + rendered_content)
        
        # Calculate statistics
        line_count = len(final_content.splitlines())
        file_size = len(final_content.encode('utf-8'))
        
        # Create output directory if it doesn't exist
        output_dir = os.path.dirname(output_path)
        if output_dir and not os.path.exists(output_dir):
            os.makedirs(output_dir, exist_ok=True)

        # Write to file
        with open(output_path, 'w', encoding='utf-8') as output_file:
            output_file.write(final_content)
        print(f"  ✅ File generated: {output_path}")
        
        return {
            'success': True,
            'output_path': output_path,
            'line_count': line_count,
            'file_size': file_size
        }
    else:
        # Draft mode: print content
        print(f"  📄 Generated content (draft mode):")
        print("=" * 80)
        print(rendered_content)
        print("=" * 80)
        
        # Calculate statistics for draft mode
        line_count = len(rendered_content.splitlines())
        file_size = len(rendered_content.encode('utf-8'))
        
        return {
            'success': True,
            'output_path': output_path,
            'line_count': line_count,
            'file_size': file_size
        }


############################### CLI stuff ###############################

def main():
    parser = argparse.ArgumentParser(
        description="Generates .java files from .jinja2 templates."
    )
    parser.add_argument(
        "target",
        help="Path to a directory (to process all .jinja2 files in Templates/) or a .jinja2 file"
    )
    parser.add_argument(
        "--prod",
        action="store_true",
        help="Production mode: replaces generated files"
    )
    parser.add_argument(
        "--draft",
        action="store_true",
        help="Draft mode: displays only the result without writing a file"
    )
    parser.add_argument(
        "--report",
        action="store_true",
        help="Display statistics report (number of lines and file size) at the end"
    )
    args = parser.parse_args()
    
    # Validate mode arguments
    if args.prod and args.draft:
        parser.error("--prod and --draft cannot be used simultaneously")
    if not args.prod and not args.draft:
        parser.error("You must specify either --prod or --draft")
    
    is_prod = args.prod
    target_path = os.path.abspath(args.target)
    
    if not os.path.exists(target_path):
        print(f"❌ Error: The path '{target_path}' does not exist")
        return
    
    templates_to_process = []
    
    if os.path.isfile(target_path):
        # Single file mode
        if not target_path.endswith('.jinja2'):
            print(f"❌ Error: The file '{target_path}' is not a .jinja2 file")
            return
        templates_to_process = [target_path]
        base_dir = os.path.dirname(os.path.dirname(target_path))  # Go up from Templates/ to folder
    elif os.path.isdir(target_path):
        # Directory mode: find all .jinja2 files in Templates/ subdirectory
        templates_dir = os.path.join(target_path, 'Templates')
        if not os.path.exists(templates_dir):
            print(f"❌ Error: The 'Templates' directory does not exist in '{target_path}'")
            return
        
        # Find all .jinja2 files
        for root, dirs, files in os.walk(templates_dir):
            for file in files:
                if file.endswith('.jinja2'):
                    templates_to_process.append(os.path.join(root, file))
        
        if not templates_to_process:
            print(f"⚠️  No .jinja2 files found in '{templates_dir}'")
            return
        
        base_dir = target_path
    else:
        print(f"❌ Error: '{target_path}' is neither a file nor a directory")
        return
    
    print(f"Mode: {'Production' if is_prod else 'Draft'}")
    print(f"Base directory: {base_dir}")
    print(f"Templates to process: {len(templates_to_process)}\n")
    
    # Process each template
    success_count = 0
    stats_list = []
    for template_path in templates_to_process:
        result = process_template(template_path, base_dir, is_prod)
        if result and result.get('success'):
            success_count += 1
            if args.report:
                stats_list.append(result)
        print()
    
    print(f"✅ {success_count}/{len(templates_to_process)} template(s) processed successfully")
    
    # Display statistics only if --report is passed
    if args.report and stats_list:
        print("\n" + "=" * 80)
        print("📊 Statistiques des fichiers générés")
        print("=" * 80)
        
        total_lines = 0
        total_size = 0
        
        for stat in stats_list:
            output_path = stat['output_path']
            line_count = stat['line_count']
            file_size = stat['file_size']
            total_lines += line_count
            total_size += file_size
            
            # Format file size
            if file_size < 1024:
                size_str = f"{file_size} B"
            elif file_size < 1024 * 1024:
                size_str = f"{file_size / 1024:.2f} KB"
            else:
                size_str = f"{file_size / (1024 * 1024):.2f} MB"
            
            print(f"  📄 {os.path.basename(output_path)}")
            print(f"     Lignes: {line_count:,}")
            print(f"     Taille: {size_str}")
            print()
        
        # Display cumulative statistics
        print("-" * 80)
        if total_size < 1024:
            total_size_str = f"{total_size} B"
        elif total_size < 1024 * 1024:
            total_size_str = f"{total_size / 1024:.2f} KB"
        else:
            total_size_str = f"{total_size / (1024 * 1024):.2f} MB"
        
        print(f"  📊 Total cumulatif:")
        print(f"     Lignes: {total_lines:,}")
        print(f"     Taille: {total_size_str}")
        print("=" * 80)

if __name__ == "__main__":
    main()
