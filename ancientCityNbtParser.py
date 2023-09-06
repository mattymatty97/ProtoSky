#This script removes all blocks except those in the array from a nbt structure for inclusion in the Protosky mod. It also clears jigsaw blocks. This was made for clearing the many individual ancient city nbt structures
#  while keeping their size and reinforced deepslate.

# Put blocks in here you want to keep in the nbt structure
blocksToKeep = ["minecraft:reinforced_deepslate", "minecraft:sculk_shrieker"]

# Put blocks in here you want to be printed to console when found
blocksToTrack = ["minecraft:sculk_shrieker", "minecraft:reinforced_deepslate"]



from nbt import nbt
from nbt.nbt import *
import glob

fileNames = [input("Input nbt file to parse or nothing to parse all nbt files in src/main/resources/data/minecraft/structures/ancient_city/ directory and its subdirectories:\n")]

if len(fileNames[0]) == 0:
	#fileNames = glob.glob("src/main/resources/data/minecraft/structures/ancient_city/**/*.nbt", recursive=True) # Get all .nbt files in the current directory and subdirectories
	fileNames = glob.glob("**/*.nbt", recursive=True) # Get all .nbt files in the current directory and subdirectories

if not fileNames[0].endswith(".nbt"):
	print("Invalid file! Please choose a NBT file!")
	exit()

for fileName in fileNames:
	nbtfile = nbt.NBTFile(fileName, 'rb')

	paletteIndecesToKeep = {}
	paletteIndecesToTrack = {}
	jigsaws = {}
	oldPalletToNewPallet = {}
	outPallet = TAG_List(name="palette", type=TAG_Compound)

	# Search through the structure palette and get the indeces that are associated with blocks to keep and track. Also find jigsaw blocks for later
	index = 0
	for paletteTag in nbtfile["palette"].tags:
		block = paletteTag["Name"].value

		if block in blocksToKeep:
			paletteIndecesToKeep[index] = block
			oldPalletToNewPallet[index] = len(outPallet.tags)
			outPallet.tags.append(paletteTag)

		if block in blocksToTrack:
			paletteIndecesToTrack[index] = block

		# Keep track of jigsaws. We will want these for later
		if block == "minecraft:jigsaw":
			jigsaws[index] = paletteTag

		index += 1

	# Create an empty tag list to store the block tags that will be kept
	newBlockList = TAG_List(name="blocks", type=TAG_Compound)

	# Iterate over the blocks and add them to newBlockList if they should be kept
	for block in nbtfile["blocks"].tags:
		paletteIndex = block["state"].value

		run = False
		# Handle jigsaws
		try:
			jigsaws[paletteIndex]
			run = True
			#print("Yes")
		except:
			pass
			#print("No")
			
		if run:
			# Check if we want to keep the jigsaws final state
			if not block["nbt"]["final_state"].value in blocksToKeep:	
			# Otherwise set the final state to air
				block["nbt"]["final_state"].value = "minecraft:air"

			# Check if we already have this in the output pallet if not add it to the output pallet.
			inPallet = False
			for outputPalletEntry in outPallet.tags:
				if (outputPalletEntry == jigsaws[paletteIndex]):
					inPallet = True
					break

			if not inPallet:
				oldPalletToNewPallet[paletteIndex] = len(outPallet.tags)
				outPallet.tags.append(jigsaws[paletteIndex])

			block["state"].value = outPallet.index(jigsaws[paletteIndex])
			newBlockList.tags.append(block)


		# Check if we want to keep the block add it to the blocks list
		if paletteIndex in paletteIndecesToKeep:
			block["state"].value = oldPalletToNewPallet[paletteIndex]
			newBlockList.tags.append(block)

		# Check if we want to track this block
		if (paletteIndex in paletteIndecesToTrack):
			# Log the tracked block and its position to the console
			x = block["pos"].tags[0].value
			y = block["pos"].tags[1].value
			z = block["pos"].tags[2].value
			print(paletteIndecesToTrack[paletteIndex] + " found at position {%d, %d, %d}" % (x, y, z))

		# Track when a jigsaw will become a block we care about
		if (paletteIndex in jigsaws and block["nbt"]["final_state"].value in blocksToTrack):
			x = block["pos"].tags[0].value
			y = block["pos"].tags[1].value
			z = block["pos"].tags[2].value
			print("Jigsaw with final state " + block["nbt"]["final_state"].value + " found at position {%d, %d, %d}" % (x, y, z))


	# Replace the original block tag list with the new pruned one
	nbtfile["blocks"] = newBlockList
	# Replace the pallet with the new pruned one
	nbtfile["palette"] = outPallet

	""" print("Blocks:")
	for blockEntry in newBlockList:
		print(blockEntry)

	print("Pallet:")
	for palletEntry in outPallet:
		print(palletEntry)
 	"""
	
	# Overwrite the original nbt file
	nbtfile.write_file(fileName)
	#nbtfile.write_file(fileName + ".nb2")
	print("Successfully pruned nbt file: '" + fileName + "'")
	print()


