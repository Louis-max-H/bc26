import sys
import os
import subprocess

def main():
    args = sys.argv

    if len(args) != 3:
        print("Usage: ./{} <source directory> <destination directory>".format(sys.argv[0]))
        print("Usage: ./{} initialBot nextBot".format(sys.argv[0]))
        return

    source = args[1]
    destination = args[2]

    source = source.replace('/', '')
    destination = destination.replace('/', '')

    print("Copying {} to {}".format(source, destination))

    # Copy folder
    os.system("cp -a {}/. {}/".format(source, destination))

    # Replace all instances of source in the destination directory
    os.system(f"cd {destination} && find . -type f -exec sed -i 's|{source}|{destination}|g' {{}} +")

if __name__ == "__main__":
    main()