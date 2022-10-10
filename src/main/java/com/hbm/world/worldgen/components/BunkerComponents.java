package com.hbm.world.worldgen.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.HbmChestContents;
import com.hbm.tileentity.network.TileEntityPylonBase;
import com.hbm.world.worldgen.components.ProceduralComponents.ProceduralComponent;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraftforge.common.util.ForgeDirection;

public class BunkerComponents extends ProceduralComponents {
	
	protected static final Weight[] weightArray = new Weight[] {
			new Weight(10, -1, (list, rand, x, y, z, mode, type) -> { StructureBoundingBox box = ProceduralComponent.getComponentToAddBoundingBox(x, y, z, -3, -1, 0, 9, 6, 15, mode); //Corridor and Wide version
				if(box.minY > 10 && StructureComponent.findIntersecting(list, box) == null) return new WideCorridor(type, rand, box, mode);
				
				box = ProceduralComponent.getComponentToAddBoundingBox(x, y, z, -1, -1, 0, 5, 6, 15, mode);
				return box.minY > 10 && StructureComponent.findIntersecting(list, box) == null ? new Corridor(type, rand, box, mode) : null; }),
			new Weight(2, -1, (list, rand, x, y, z, mode, type) -> { StructureBoundingBox box = ProceduralComponent.getComponentToAddBoundingBox(x, y, z, -3, -1, 0, 9, 6, 9, mode); //Intersection and wide ver.
				if(box.minY > 10 && StructureComponent.findIntersecting(list, box) == null) return new WideIntersection(type, rand, box, mode);
				
				box = ProceduralComponent.getComponentToAddBoundingBox(x, y, z, -1, -1, 0, 5, 6, 5, mode);
				return box.minY > 10 && StructureComponent.findIntersecting(list, box) == null ? new Intersection(type, rand, box, mode) : null; }),
			new Weight(2, 5, (list, rand, x, y, z, mode, type) -> { StructureBoundingBox box = ProceduralComponent.getComponentToAddBoundingBox(x, y, z, -1, -1, 0, 5, 5, 4, mode);
				return box.minY > 10 && StructureComponent.findIntersecting(list, box) == null ? new UtilityCloset(type, rand, box, mode) : null; }) {
				public boolean canSpawnStructure(int componentAmount, int coordMode, ProceduralComponent component) {
					return (this.instanceLimit < 0 || this.instanceLimit < this.instanceLimit) && componentAmount > 10; //prevent the gimping of necessary corridors
				}
			},
	};
	
	public static void prepareComponents() {
		componentWeightList = new ArrayList();
		
		for(int i = 0; i < weightArray.length; i++) {
			weightArray[i].instancesSpawned = 0;
			componentWeightList.add(weightArray[i]);
		}
	}
	
	public static abstract class Bunker extends ProceduralComponent {
		
		boolean underwater = false;
		
		public Bunker() { }
		
		public Bunker(int componentType) {
			super(componentType);
		}
		
		protected void checkModifiers(ControlComponent original) {
			if(original instanceof Atrium)
				this.underwater = ((Atrium) original).underwater;
		}
		
		protected void placeLamp(World world, StructureBoundingBox box, Random rand, int featureX, int featureY, int featureZ) {
			if(rand.nextInt(underwater ? 5 : 3) == 0) {
				placeBlockAtCurrentPosition(world, ModBlocks.reinforced_lamp_on, 0, featureX, featureY, featureZ, box);
				placeBlockAtCurrentPosition(world, Blocks.redstone_block, 0, featureX, featureY + 1, featureZ, box);
			} else
				placeBlockAtCurrentPosition(world, ModBlocks.reinforced_lamp_off, 0, featureX, featureY, featureZ, box);
		}
		
		protected void fillWithWater(World world, StructureBoundingBox box, Random rand, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int waterLevel) {
			
			if(getYWithOffset(minY) < box.minY || getYWithOffset(maxY) > box.maxY)
				return;
			
			waterLevel += getYWithOffset(minY) - 1;
			
			for(int x = minX; x <= maxX; x++) {
				
				for(int z = minZ; z <= maxZ; z++) {
					int posX = getXWithOffset(x, z);
					int posZ = getZWithOffset(x, z);
					
					if(posX >= box.minX && posX <= box.maxX && posZ >= box.minZ && posZ <= box.maxZ) {
						for(int y = minY; y <= maxY; y++) {
							int posY = getYWithOffset(y);
							Block genTarget = world.getBlock(posX, posY, posZ);
							
							if(!genTarget.isAir(world, posX, posY, posZ))
								continue;
							
							if(posY <= waterLevel && world.getBlock(posX, posY - 1, posZ).getMaterial() != Material.air) {
								world.setBlock(posX, posY, posZ, Blocks.water, 0, 2);
								continue;
							}
							
							boolean canGenFluid = true;
							boolean canGenPlant = false;
							int canGenVine = -1;
							
							for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
								Block neighbor = world.getBlock(posX + dir.offsetX, posY + dir.offsetY, posZ + dir.offsetZ);
								boolean isSolid = neighbor.isNormalCube();
								
								switch(dir) {
								case DOWN: if(neighbor == Blocks.water) canGenPlant = true;
									if(!isSolid && neighbor != Blocks.water) canGenFluid = false;
									break;
								case UP: if(neighbor.getMaterial() != Material.air) canGenFluid = false; 
									if(isSolid) canGenVine = 0;
									break;
								case NORTH: if(isSolid) canGenVine |= 4;
									if(!isSolid && neighbor != Blocks.water) canGenFluid = false;
									break;
								case SOUTH: if(isSolid) canGenVine |= 1;
									if(!isSolid && neighbor != Blocks.water) canGenFluid = false;
									break;
								case EAST: if(isSolid) canGenVine |= 8;
									if(!isSolid && neighbor != Blocks.water) canGenFluid = false;
									break;
								case WEST: if(isSolid) canGenVine |= 2;
									if(!isSolid && neighbor != Blocks.water) canGenFluid = false;
									break;
								default: //shut the fuck up!
									if(!isSolid && neighbor != Blocks.water) canGenFluid = false;
									break;
								}
							}
							
							if(canGenFluid)
								world.setBlock(posX, posY, posZ, Blocks.water);
							else {
								 if(canGenVine != -1) {
									if(rand.nextInt(3) == 0)
										canGenVine |= 1 << rand.nextInt(4);
									
									world.setBlock(posX, posY, posZ, Blocks.vine, canGenVine, 2);
									
									if(canGenVine > 0) {
										int i = posY;
										while(world.getBlock(posX, --i, posZ).getMaterial() == Material.air)
											world.setBlock(posX, i, posZ, Blocks.vine, canGenVine, 2);
									}
								 }
								 
								 if(canGenPlant) {
										/*Block belowNeighbor = world.getBlock(posX, posY - 2, posZ);
										int bound = !belowNeighbor.isNormalCube() ? 10 : 10; //reeds
										int value = rand.nextInt(bound);
										
										if(value <= 0) {*/
											int rY = posY + rand.nextInt(10) - rand.nextInt(10);
											if(rY == posY)
												world.setBlock(posX, posY, posZ, Blocks.waterlily, 0, 2);
										//}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static class UtilityCloset extends Bunker {
		
		boolean energy = false; //if false, this is a water closet. if true, this is an energy closet
		boolean hasLoot = false;
		
		public UtilityCloset() { }
		
		public UtilityCloset(int componentType, Random rand, StructureBoundingBox box, int coordBaseMode) {
			super(componentType);
			this.coordBaseMode = coordBaseMode;
			this.boundingBox = box;
			
			energy = rand.nextBoolean();
			hasLoot = rand.nextInt(3) == 0;
		}
		
		@Override
		public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
			if(!underwater && isLiquidInStructureBoundingBox(world, boundingBox)) {
				return false;
			} else {
				
				fillWithAir(world, box, 1, 1, 1, 3, 3, 2);
				//Floor
				placeBlockAtCurrentPosition(world, ModBlocks.deco_titanium, 0, 2, 0, 0, box);
				fillWithBlocks(world, box, 1, 0, 1, 3, 0, 2, ModBlocks.deco_titanium);
				//Wall
				fillWithBlocks(world, box, 0, 1, 1, 0, 1, 2, ModBlocks.reinforced_brick);
				fillWithBlocks(world, box, 0, 2, 1, 0, 2, 2, ModBlocks.reinforced_stone);
				fillWithBlocks(world, box, 0, 3, 1, 0, 3, 2, ModBlocks.reinforced_brick);
				placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 1, 1, 0, box);
				placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 1, 2, 0, box);
				fillWithBlocks(world, box, 1, 3, 0, 3, 3, 0, ModBlocks.reinforced_brick);
				placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 3, 1, 0, box);
				placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 3, 2, 0, box);
				fillWithBlocks(world, box, 4, 1, 1, 4, 1, 2, ModBlocks.reinforced_brick);
				fillWithBlocks(world, box, 4, 2, 1, 4, 2, 2, ModBlocks.reinforced_stone);
				fillWithBlocks(world, box, 4, 3, 1, 4, 3, 2, ModBlocks.reinforced_brick);
				fillWithBlocks(world, box, 1, 1, 3, 3, 1, 3, ModBlocks.reinforced_brick);
				fillWithBlocks(world, box, 1, 2, 3, 3, 2, 3, ModBlocks.reinforced_stone);
				fillWithBlocks(world, box, 1, 3, 3, 3, 3, 3, ModBlocks.reinforced_brick);
				//Ceiling
				fillWithBlocks(world, box, 1, 4, 1, 3, 4, 2, ModBlocks.reinforced_brick);
				
				int decoMetaS = getDecoMeta(2);
				int decoMetaN = getDecoMeta(3);
				int decoMetaW = getDecoMeta(4);
				
				if(energy) {
					placeBlockAtCurrentPosition(world, ModBlocks.red_wire_coated, 0, 0, 3, 2, box);
					placeBlockAtCurrentPosition(world, ModBlocks.machine_transformer, 0, 1, 3, 2, box);
					placeBlockAtCurrentPosition(world, ModBlocks.red_connector, getDecoMeta(5), 2, 3, 2, box);
					fillWithMetadataBlocks(world, box, 2, 1, 2, 2, 2, 2, ModBlocks.red_connector, decoMetaW);
					
					makeConnection(world, 2, 3, 2, 2, 2, 2);
					makeConnection(world, 2, 2, 2, 2, 1, 2);
					
					fillWithMetadataBlocks(world, box,3, 1, 1, 3, 2, 1, ModBlocks.steel_wall, decoMetaS);
					placeBlockAtCurrentPosition(world, ModBlocks.steel_roof, 0, 3, 3, 2, box);
					placeBlockAtCurrentPosition(world, ModBlocks.cable_diode, decoMetaS, 3, 1, 2, box);
					placeBlockAtCurrentPosition(world, ModBlocks.cable_diode, decoMetaW, 3, 2, 2, box);
					placeBlockAtCurrentPosition(world, ModBlocks.deco_red_copper, 0, 3, 1, 3, box);
					placeBlockAtCurrentPosition(world, ModBlocks.deco_red_copper, 0, 4, 2, 2, box);
					
					int cabinetMeta = getDecoModelMeta(0);
					if(hasLoot)
						generateInvContents(world, box, rand, ModBlocks.filing_cabinet, cabinetMeta, 1, 1, 2, HbmChestContents.filingCabinet, 4);
				} else {
					fillWithMetadataBlocks(world, box, 1, 1, 2, 2, 1, 2, ModBlocks.deco_pipe_quad_green_rusted, getPillarMeta(4));
					placeBlockAtCurrentPosition(world, ModBlocks.machine_boiler_off, decoMetaN, 3, 1, 2, box);
					fillWithBlocks(world, box, 3, 2, 2, 3, 3, 2, ModBlocks.deco_pipe_rusted);
					
					int cabinetMeta = getDecoModelMeta(3);
					if(hasLoot)
						generateInvContents(world, box, rand, ModBlocks.filing_cabinet, cabinetMeta, 1, 1, 1, HbmChestContents.filingCabinet, 4);
				}
				
				//Door
				placeDoor(world, box, ModBlocks.door_bunker, 1, 2, 1, 0);
				
				fillWithCobwebs(world, box, rand, 1, 1, 1, 3, 3, 2);
				
				return true;
			}
		}
		
		protected void makeConnection(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
			int posX1 = getXWithOffset(x1, z1);
			int posY1 = getYWithOffset(y1);
			int posZ1 = getZWithOffset(x1, z1);
			
			int posX2 = getXWithOffset(x2, z2);
			int posY2 = getYWithOffset(y2);
			int posZ2 = getZWithOffset(x2, z2);
			
			TileEntity tile1 = world.getTileEntity(posX1, posY1, posZ1);
			TileEntity tile2 = world.getTileEntity(posX2, posY2, posZ2);
			if(tile1 instanceof TileEntityPylonBase && tile2 instanceof TileEntityPylonBase) {
				TileEntityPylonBase pylon1 = (TileEntityPylonBase)tile1;
				pylon1.addConnection(posX2, posY2, posZ2);
				TileEntityPylonBase pylon2 = (TileEntityPylonBase)tile2;
				pylon2.addConnection(posX1, posY1, posZ1);
			}
		}
	}
	
	public static class Atrium extends ControlComponent {
		
		public boolean underwater = false;
		
		public Atrium() { }
		
		public Atrium(int componentType, Random rand, int posX, int posZ) { //TODO: change basically everything about this component
			super(componentType);
			this.coordBaseMode = rand.nextInt(4);
			this.boundingBox = new StructureBoundingBox(posX, 64, posZ, posX + 8, 68, posZ + 8);
		}
		
		@Override
		public void buildComponent(ControlComponent original, List components, Random rand) {
			
			StructureComponent component = getNextComponentNormal(original, components, rand, 3, 1);
			System.out.println("ComponentPZ:" + component);
			
			StructureComponent componentN = getNextComponentNX(original, components, rand, 3, 1);
			System.out.println("ComponentNX:" + componentN);
			
			StructureComponent componentP = getNextComponentPX(original, components, rand, 3, 1);
			System.out.println("ComponentPX:" + componentP);
		}
		
		@Override
		public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
			return true;
		}
	}
	
	public static class Corridor extends Bunker {
		
		boolean expandsNX = false;
		boolean expandsPX = false;
		boolean extendsPZ = true;
		
		public Corridor() { }
		
		public Corridor(int componentType, Random rand, StructureBoundingBox box, int coordBaseMode) {
			super(componentType);
			this.coordBaseMode = coordBaseMode;
			this.boundingBox = box;
			
		}
		
		protected void func_143012_a(NBTTagCompound data) {
			super.func_143012_a(data);
			data.setBoolean("expandsNX", expandsNX);
			data.setBoolean("expandsPX", expandsPX);
			data.setBoolean("extendsPZ", extendsPZ);
		}
		
		protected void func_143011_b(NBTTagCompound data) {
			super.func_143011_b(data);
			expandsNX = data.getBoolean("expandsNX");
			expandsPX = data.getBoolean("expandsPX");
			extendsPZ = data.getBoolean("extendsPZ");
		}
		
		@Override
		public void buildComponent(ControlComponent original, List components, Random rand) {
			checkModifiers(original);
			
			StructureComponent component = getNextComponentNormal(original, components, rand, 1, 1);
			extendsPZ = component != null;
			
			if(rand.nextInt(3) > 0) {
				StructureComponent componentN = getNextComponentNX(original, components, rand, 6, 1);
				expandsNX = componentN != null;
			}
			
			if(rand.nextInt(3) > 0) {
				StructureComponent componentP = getNextComponentPX(original, components, rand, 6, 1);
				expandsPX = componentP != null;
			}
		}
		
		@Override
		public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
			
			if(!underwater && isLiquidInStructureBoundingBox(world, boundingBox)) {
				return false;
			} else {
				int end = extendsPZ ? 14 : 13;
				
				fillWithAir(world, box, 1, 1, 0, 3, 3, end);				
				fillWithBlocks(world, box, 1, 0, 0, 3, 0, end, ModBlocks.deco_titanium);
				
				//Walls
				for(int x = 0; x <= 4; x += 4) {
					fillWithBlocks(world, box, x, 1, 0, x, 1, 4, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, x, 1, 10, x, 1, end, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, x, 2, 0, x, 2, 4, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, x, 2, 10, x, 2, end, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, x, 3, 10, x, 3, end, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, x, 3, 0, x, 3, 4, ModBlocks.reinforced_brick);
				}
				
				if(!extendsPZ) {
					fillWithBlocks(world, box, 1, 1, 14, 3, 1, 14, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 1, 2, 14, 3, 2, 14, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 1, 3, 14, 3, 3, 14, ModBlocks.reinforced_brick);
				}
				
				//ExpandsNX
				if(expandsNX) {
					fillWithBlocks(world, box, 0, 0, 6, 0, 0, 8, ModBlocks.deco_titanium); //Floor
					fillWithBlocks(world, box, 0, 1, 5, 0, 3, 5, ModBlocks.concrete_pillar); //Walls
					fillWithBlocks(world, box, 0, 1, 9, 0, 3, 9, ModBlocks.concrete_pillar);
					fillWithAir(world, box, 0, 1, 6, 0, 3, 8);
					fillWithBlocks(world, box, 0, 4, 6, 0, 4, 8, ModBlocks.reinforced_brick); //Ceiling
				} else {
					fillWithBlocks(world, box, 0, 1, 5, 0, 1, 9, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 0, 2, 5, 0, 2, 9, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 0, 3, 5, 0, 3, 9, ModBlocks.reinforced_brick);
				}
				
				//ExpandsPX
				if(expandsPX) {
					fillWithBlocks(world, box, 4, 0, 6, 4, 0, 8, ModBlocks.deco_titanium);
					fillWithBlocks(world, box, 4, 1, 5, 4, 3, 5, ModBlocks.concrete_pillar);
					fillWithBlocks(world, box, 4, 1, 9, 4, 3, 9, ModBlocks.concrete_pillar);
					fillWithAir(world, box, 4, 1, 6, 4, 3, 8);
					fillWithBlocks(world, box, 4, 4, 6, 4, 4, 8, ModBlocks.reinforced_brick);
				} else {
					fillWithBlocks(world, box, 4, 1, 5, 4, 1, 9, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 4, 2, 5, 4, 2, 9, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 4, 3, 5, 4, 3, 9, ModBlocks.reinforced_brick);
				}
				
				//Ceiling
				fillWithBlocks(world, box, 1, 4, 0, 1, 4, end, ModBlocks.reinforced_brick);
				fillWithBlocks(world, box, 3, 4, 0, 3, 4, end, ModBlocks.reinforced_brick);
				int pillarMeta = getPillarMeta(8);
				for(int i = 0; i <= 12; i += 3) {
					placeBlockAtCurrentPosition(world, ModBlocks.concrete_pillar, pillarMeta, 2, 4, i, box);
					placeLamp(world, box, rand, 2, 4, i + 1);
					
					if(extendsPZ || i < 12)
						placeBlockAtCurrentPosition(world, ModBlocks.concrete_pillar, pillarMeta, 2, 4, i + 2, box);
				}
				
				if(underwater) {
					fillWithWater(world, box, rand, 1, 1, 0, 3, 3, end, 1);
					if(expandsNX) fillWithWater(world, box, rand, 0, 1, 6, 0, 3, 8, 1);
					if(expandsPX) fillWithWater(world, box, rand, 4, 1, 6, 4, 3, 8, 1);
				} else
					fillWithCobwebs(world, box, rand, expandsNX ? 0 : 1, 1, 0, expandsPX ? 4 : 3, 3, end);
				
				return true;
			}
		}
		
	}
	
	interface Wide { } //now you may ask yourself - where is that beautiful house? you may ask yourself - where does that highway go to?
	//you may ask yourself - am i right, am i wrong? you may say to yourself - my god, no multiple inheritance to be done!
	
	public static class WideCorridor extends Corridor implements Wide {
		
		boolean bulkheadNZ = true;
		boolean bulkheadPZ = true;
		
		public WideCorridor() { }
		
		public WideCorridor(int componentType, Random rand, StructureBoundingBox box, int coordBaseMode) {
			super(componentType, rand, box, coordBaseMode);
		}
		
		@Override
		public void buildComponent(ControlComponent original, List components, Random rand) {
			checkModifiers(original);
			
			StructureComponent component = getNextComponentNormal(original, components, rand, 3, 1);
			extendsPZ = component != null;
			
			if(component instanceof Wide) {
				bulkheadPZ = false;
				
				if(component instanceof WideCorridor) {
					WideCorridor corridor = (WideCorridor) component;
					corridor.bulkheadNZ = rand.nextInt(4) == 0;
				}
			}
			
			if(rand.nextInt(3) > 0) {
				StructureComponent componentN = getNextComponentNX(original, components, rand, 6, 1);
				expandsNX = componentN != null;
			}
			
			if(rand.nextInt(3) > 0) {
				StructureComponent componentP = getNextComponentPX(original, components, rand, 6, 1);
				expandsPX = componentP != null;
			}
		}
		
		@Override
		public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
			
			if(!underwater && isLiquidInStructureBoundingBox(world, boundingBox)) {
				return false;
			} else {
				int begin = bulkheadNZ ? 1 : 0;
				int end =  bulkheadPZ ? 13 : 14; //for the bulkhead
				int endExtend = !extendsPZ ? 13 : 14; //for parts that would be cut off if it doesn't extend further
				
				fillWithAir(world, box, 1, 1, begin, 7, 3, end);
				
				//Floor
				fillWithBlocks(world, box, 1, 0, begin, 1, 0, end, ModBlocks.deco_titanium);
				fillWithBlocks(world, box, 2, 0, begin, 2, 0, end, ModBlocks.tile_lab);
				fillWithBlocks(world, box, 3, 0, 0, 5, 0, endExtend, ModBlocks.deco_titanium);
				fillWithBlocks(world, box, 6, 0, begin, 6, 0, end, ModBlocks.tile_lab);
				fillWithBlocks(world, box, 7, 0, begin, 7, 0, end, ModBlocks.deco_titanium);
				
				int pillarMeta = getPillarMeta(8);
				//Walls
				if(expandsNX) {
					fillWithBlocks(world, box, 0, 1, begin, 0, 1, 4, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 0, 2, begin, 0, 2, 4, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 0, 3, begin, 0, 3, 4, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 0, 1, 10, 0, 1, end, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 0, 2, 10, 0, 2, end, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 0, 3, 10, 0, 3, end, ModBlocks.reinforced_brick);
					
					fillWithBlocks(world, box, 0, 0, 6, 0, 0, 8, ModBlocks.deco_titanium);
					fillWithBlocks(world, box, 0, 1, 5, 0, 3, 5, ModBlocks.concrete_pillar);
					fillWithBlocks(world, box, 0, 1, 9, 0, 3, 9, ModBlocks.concrete_pillar);
					fillWithMetadataBlocks(world, box, 0, 4, 6, 0, 4, 8, ModBlocks.concrete_pillar, pillarMeta);
					fillWithAir(world, box, 0, 1, 6, 0, 3, 8);
					
				} else {
					fillWithBlocks(world, box, 0, 1, begin, 0, 1, end, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 0, 2, begin, 0, 2, end, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 0, 3, begin, 0, 3, end, ModBlocks.reinforced_brick);
				}
				
				if(expandsPX) {
					fillWithBlocks(world, box, 8, 1, begin, 8, 1, 4, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 8, 2, begin, 8, 2, 4, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 8, 3, begin, 8, 3, 4, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 8, 1, 10, 8, 1, end, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 8, 2, 10, 8, 2, end, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 8, 3, 10, 8, 3, end, ModBlocks.reinforced_brick);
					
					fillWithBlocks(world, box, 8, 0, 6, 8, 0, 8, ModBlocks.deco_titanium);
					fillWithBlocks(world, box, 8, 1, 5, 8, 3, 5, ModBlocks.concrete_pillar);
					fillWithBlocks(world, box, 8, 1, 9, 8, 3, 9, ModBlocks.concrete_pillar);
					fillWithMetadataBlocks(world, box, 8, 4, 6, 8, 4, 8, ModBlocks.concrete_pillar, pillarMeta);
					fillWithAir(world, box, 8, 1, 6, 8, 3, 8);
					
				} else {
					fillWithBlocks(world, box, 8, 1, begin, 8, 1, end, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 8, 2, begin, 8, 2, end, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 8, 3, begin, 8, 3, end, ModBlocks.reinforced_brick);
				}
				
				if(bulkheadNZ) {
					fillWithBlocks(world, box, 1, 1, 0, 2, 1, 0, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 1, 2, 0, 2, 2, 0, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 1, 3, 0, 2, 3, 0, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 6, 1, 0, 7, 1, 0, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 6, 2, 0, 7, 2, 0, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 6, 3, 0, 7, 3, 0, ModBlocks.reinforced_brick);
					fillWithAir(world, box, 3, 1, 0, 5, 3, 0);
				}
				
				if(bulkheadPZ) {
					fillWithBlocks(world, box, 1, 1, 14, 2, 1, 14, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 1, 2, 14, 2, 2, 14, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 1, 3, 14, 2, 3, 14, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 6, 1, 14, 7, 1, 14, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 6, 2, 14, 7, 2, 14, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 6, 3, 14, 7, 3, 14, ModBlocks.reinforced_brick);
					
					if(!extendsPZ) {
						fillWithBlocks(world, box, 3, 1, 14, 5, 1, 14, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 3, 2, 14, 5, 2, 14, ModBlocks.reinforced_stone);
						fillWithBlocks(world, box, 3, 3, 14, 5, 3, 14, ModBlocks.reinforced_brick);
					} else
						fillWithAir(world, box, 3, 1, 14, 5, 3, 14);
				}
				
				//Ceiling
				fillWithBlocks(world, box, 1, 4, begin, 1, 4, end, ModBlocks.reinforced_brick);
				fillWithMetadataBlocks(world, box, 2, 4, begin, 2, 4, end, ModBlocks.concrete_pillar, pillarMeta);
				fillWithBlocks(world, box, 3, 4, 0, 3, 4, endExtend, ModBlocks.reinforced_brick);
				fillWithBlocks(world, box, 5, 4, 0, 5, 4, endExtend, ModBlocks.reinforced_brick);
				fillWithMetadataBlocks(world, box, 6, 4, begin, 6, 4, end, ModBlocks.concrete_pillar, pillarMeta);
				fillWithBlocks(world, box, 7, 4, begin, 7, 4, end, ModBlocks.reinforced_brick);
				
				for(int i = 0; i <= 12; i += 3) {
					placeBlockAtCurrentPosition(world, ModBlocks.concrete_pillar, pillarMeta, 4, 4, i, box);
					placeLamp(world, box, rand, 4, 4, i + 1);
					
					if(extendsPZ || i < 12)
						placeBlockAtCurrentPosition(world, ModBlocks.concrete_pillar, pillarMeta, 4, 4, i + 2, box);
				}
				
				if(underwater) {
					fillWithWater(world, box, rand, 1, 1, 0, 7, 3, endExtend, 1);
					if(expandsNX) fillWithWater(world, box, rand, 0, 1, 6, 0, 3, 8, 1);
					if(expandsPX) fillWithWater(world, box, rand, 8, 1, 6, 8, 3, 8, 1);
				} else
					fillWithCobwebs(world, box, rand, expandsNX ? 0 : 1, 1, 0, expandsPX ? 8 : 7, 3, endExtend);
								
				return true;
			}
		}
	}
	
	public static class Turn extends ProceduralComponent {
		
		public Turn() { }
		
		public Turn(int componentType, Random rand, StructureBoundingBox box, int coordBaseMode) {
			super(componentType);
			this.coordBaseMode = coordBaseMode;
			this.boundingBox = box;
			
		}
		
		@Override
		public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
			return true;
		}
	}
	
	public static class WideTurn extends Turn implements Wide {
		
		public WideTurn() { }
		
		public WideTurn(int componentType, Random rand, StructureBoundingBox box, int coordBaseMode) {
			super(componentType, rand, box, coordBaseMode);
		}
		
		@Override
		public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
			return true;
		}
	}
	
	public static class Intersection extends Bunker {
		
		boolean opensNX = false;
		boolean opensPX = false;
		boolean opensPZ = false;
		
		public Intersection() { }
		
		public Intersection(int componentType, Random rand, StructureBoundingBox box, int coordBaseMode) {
			super(componentType);
			this.coordBaseMode = coordBaseMode;
			this.boundingBox = box;
			
		}
		
		@Override
		public void buildComponent(ControlComponent original, List components, Random rand) {
			checkModifiers(original);
			
			StructureComponent component = getNextComponentNormal(original, components, rand, 1, 1);
			opensPZ = component != null;
			
			StructureComponent componentN = getNextComponentNX(original, components, rand, 1, 1);
			opensNX = componentN != null;
			
			StructureComponent componentP = getNextComponentPX(original, components, rand, 1, 1);
			opensPX = componentP != null;
		}
		
		protected void func_143012_a(NBTTagCompound data) {
			super.func_143012_a(data);
			data.setBoolean("opensNX", opensNX);
			data.setBoolean("opensPX", opensPX);
			data.setBoolean("opensPZ", opensPZ);
		}
		
		protected void func_143011_b(NBTTagCompound data) {
			super.func_143011_b(data);
			opensNX = data.getBoolean("opensNX");
			opensPX = data.getBoolean("opensPX");
			opensPZ = data.getBoolean("opensPZ");
		}
		
		@Override
		public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
			if(!underwater && isLiquidInStructureBoundingBox(world, boundingBox)) {
				return false;
			} else {
				
				fillWithAir(world, box, 1, 1, 0, 3, 3, 3);
				//Floor
				fillWithBlocks(world, box, 1, 0, 0, 3, 0, 3, ModBlocks.deco_titanium);
				//Ceiling
				int pillarMetaWE = getPillarMeta(4);
				int pillarMetaNS = getPillarMeta(8);
				
				fillWithBlocks(world, box, 3, 4, 0, 3, 4, 1, ModBlocks.reinforced_brick);
				fillWithMetadataBlocks(world, box, 2, 4, 0, 2, 4, 1, ModBlocks.concrete_pillar, pillarMetaNS);
				fillWithBlocks(world, box, 1, 4, 0, 1, 4, 1, ModBlocks.reinforced_brick);
				
				placeLamp(world, box, rand, 2, 4, 2);
				
				if(opensPZ) {
					fillWithBlocks(world, box, 1, 0, 4, 3, 0, 4, ModBlocks.deco_titanium); //Floor
					fillWithBlocks(world, box, 1, 4, 3, 1, 4, 4, ModBlocks.reinforced_brick); //Ceiling
					fillWithMetadataBlocks(world, box, 2, 4, 3, 2, 4, 4, ModBlocks.concrete_pillar, pillarMetaNS);
					fillWithBlocks(world, box, 3, 4, 3, 3, 4, 4, ModBlocks.reinforced_brick);
					fillWithAir(world, box, 1, 1, 4, 3, 3, 4); //Opening
				} else {
					fillWithBlocks(world, box, 1, 4, 3, 3, 4, 3, ModBlocks.reinforced_brick); //Ceiling
					fillWithBlocks(world, box, 1, 1, 4, 3, 1, 4, ModBlocks.reinforced_brick); //Wall
					fillWithBlocks(world, box, 1, 2, 4, 3, 2, 4, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 1, 3, 4, 3, 3, 4, ModBlocks.reinforced_brick);
				}
				
				if(opensNX) {
					fillWithBlocks(world, box, 0, 0, 1, 0, 0, 3, ModBlocks.deco_titanium); //Floor
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 4, 1, box); //Ceiling
					fillWithMetadataBlocks(world, box, 0, 4, 2, 1, 4, 2, ModBlocks.concrete_pillar, pillarMetaWE);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 4, 3, box);
					fillWithAir(world, box, 0, 1, 1, 0, 3, 3); //Opening
				} else {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 1, 4, 2, box); //Ceiling
					fillWithBlocks(world, box, 0, 1, 1, 0, 1, 3, ModBlocks.reinforced_brick); //Wall
					fillWithBlocks(world, box, 0, 2, 1, 0, 2, 3, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 0, 3, 1, 0, 3, 3, ModBlocks.reinforced_brick);
				}
				
				if(opensPX) {
					fillWithBlocks(world, box, 4, 0, 1, 4, 0, 3, ModBlocks.deco_titanium); //Floor
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 4, 4, 1, box); //Ceiling
					fillWithMetadataBlocks(world, box, 3, 4, 2, 4, 4, 2, ModBlocks.concrete_pillar, pillarMetaWE);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 4, 4, 3, box);
					fillWithAir(world, box, 4, 1, 1, 4, 3, 3); //Opening
				} else {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 3, 4, 2, box);
					fillWithBlocks(world, box, 4, 1, 1, 4, 1, 3, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 4, 2, 1, 4, 2, 3, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 4, 3, 1, 4, 3, 3, ModBlocks.reinforced_brick);
				}
				
				//Pillars
				if(opensNX)
					fillWithBlocks(world, box, 0, 1, 0, 0, 3, 0, ModBlocks.concrete_pillar);
				else {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 1, 0, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 0, 2, 0, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 3, 0, box);
				}
				
				if(opensPX)
					fillWithBlocks(world, box, 4, 1, 0, 4, 3, 0, ModBlocks.concrete_pillar);
				else {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 4, 1, 0, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 4, 2, 0, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 4, 3, 0, box);
				}
				
				if(opensNX && opensPZ)
					fillWithBlocks(world, box, 0, 1, 4, 0, 3, 4, ModBlocks.concrete_pillar);
				else if(opensNX || opensPZ) {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 1, 4, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 0, 2, 4, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 3, 4, box);
				}
				
				if(opensPX && opensPZ)
					fillWithBlocks(world, box, 4, 1, 4, 4, 3, 4, ModBlocks.concrete_pillar);
				else if(opensPX || opensPZ) {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 4, 1, 4, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 4, 2, 4, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 4, 3, 4, box);
				}
				
				if(underwater) {
					fillWithWater(world, box, rand, 1, 1, 0, 3, 3, opensPX ? 4 : 3, 1);
					if(opensNX) fillWithWater(world, box, rand, 0, 1, 1, 0, 3, 3, 1);
					if(opensPX) fillWithWater(world, box, rand, 4, 1, 1, 4, 3, 3, 1);
				} else
					fillWithCobwebs(world, box, rand, opensNX ? 0 : 1, 1, 0, opensPX ? 4 : 3, 3, opensPZ ? 4 : 3);
				
				return true;
			}
		}
	}
	
	public static class WideIntersection extends Intersection implements Wide {
		
		boolean bulkheadNZ = true;
		boolean bulkheadPZ = true;
		boolean bulkheadNX = true;
		boolean bulkheadPX = true;
		
		public WideIntersection() { }
		
		public WideIntersection(int componentType, Random rand, StructureBoundingBox box, int coordBaseMode) {
			super(componentType, rand, box, coordBaseMode);
		}
		
		protected void func_143012_a(NBTTagCompound data) {
			super.func_143012_a(data);
			data.setBoolean("bulkheadNZ", bulkheadNZ);
			data.setBoolean("bulkheadPZ", bulkheadPZ);
			data.setBoolean("opensNX", opensNX);
			data.setBoolean("opensPX", opensPX);
		}
		
		protected void func_143011_b(NBTTagCompound data) {
			super.func_143011_b(data);
			bulkheadNZ = data.getBoolean("bulkheadNZ");
			bulkheadPZ = data.getBoolean("bulkheadPZ");
			bulkheadNX = data.getBoolean("bulkheadNX");
			bulkheadPX = data.getBoolean("bulkheadPX");
		}
		
		@Override
		public void buildComponent(ControlComponent original, List components, Random rand) {
			checkModifiers(original);
			
			StructureComponent component = getNextComponentNormal(original, components, rand, 3, 1);
			opensPZ = component != null;
			
			if(component instanceof Wide) {
				bulkheadPZ = false;
				
				if(component instanceof WideCorridor) {
					WideCorridor corridor = (WideCorridor) component;
					corridor.bulkheadNZ = rand.nextInt(4) == 0;
				}
			}
			
			StructureComponent componentN = getNextComponentNX(original, components, rand, 3, 1);
			opensNX = componentN != null;
			
			if(componentN instanceof Wide) {
				bulkheadNX = false;
				
				if(componentN instanceof WideCorridor) {
					WideCorridor corridor = (WideCorridor) componentN;
					corridor.bulkheadNZ = rand.nextInt(4) == 0;
				}
			}
			
			StructureComponent componentP = getNextComponentPX(original, components, rand, 3, 1);
			opensPX = componentP != null;
			
			if(componentP instanceof Wide) {
				bulkheadPX = false;
				
				if(componentP instanceof WideCorridor) {
					WideCorridor corridor = (WideCorridor) componentP;
					corridor.bulkheadNZ = rand.nextInt(4) == 0;
				}
			}
		}
		
		@Override
		public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
			if(!underwater && isLiquidInStructureBoundingBox(world, boundingBox)) {
				return false;
			} else {
				
				int start = bulkheadNZ ? 1 : 0;
				int end = bulkheadPZ ? 7 : 8;
				int right = bulkheadNX ? 1 : 0;
				int left = bulkheadPX ? 7 : 8;
				
				int pillarMetaNS = getPillarMeta(8);
				int pillarMetaWE = getPillarMeta(4);
				
				fillWithAir(world, box, 1, 1, 0, 7, 3, end);
				//Floor
				fillWithBlocks(world, box, 3, 0, 0, 5, 0, 1, ModBlocks.deco_titanium);
				fillWithBlocks(world, box, 2, 0, start, 2, 0, 6, ModBlocks.tile_lab);
				fillWithBlocks(world, box, 3, 0, 2, 5, 0, 2, ModBlocks.tile_lab);
				fillWithBlocks(world, box, 3, 0, 6, 5, 0, 6, ModBlocks.tile_lab);
				fillWithBlocks(world, box, 6, 0, start, 6, 0, 6, ModBlocks.tile_lab);
				fillWithBlocks(world, box, 3, 0, 3, 5, 0, 5, ModBlocks.deco_titanium);
				//Wall
				if(!bulkheadNZ || (opensNX && !bulkheadNX)) {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 1, 0, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 0, 2, 0, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 3, 0, box);
				}
				
				if(!bulkheadNZ || (opensPX && !bulkheadPX)) {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 8, 1, 0, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 8, 2, 0, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 8, 3, 0, box);
				}
				//Ceiling
				fillWithBlocks(world, box, 1, 4, start, 1, 4, 1, ModBlocks.reinforced_brick);
				fillWithMetadataBlocks(world, box, 2, 4, start, 2, 4, 1, ModBlocks.concrete_pillar, pillarMetaNS);
				fillWithBlocks(world, box, 3, 4, 0, 3, 4, 3, ModBlocks.reinforced_brick);
				fillWithMetadataBlocks(world, box, 4, 4, 0, 4, 4, 3, ModBlocks.concrete_pillar, pillarMetaNS);
				fillWithBlocks(world, box, 5, 4, 0, 5, 4, 3, ModBlocks.reinforced_brick);
				fillWithMetadataBlocks(world, box, 6, 4, start, 6, 4, 1, ModBlocks.concrete_pillar, pillarMetaNS);
				fillWithBlocks(world, box, 7, 4, start, 7, 4, 1, ModBlocks.reinforced_brick);
				placeLamp(world, box, rand, 2, 4, 2);
				placeLamp(world, box, rand, 2, 4, 6);
				placeLamp(world, box, rand, 4, 4, 4);
				placeLamp(world, box, rand, 6, 4, 2);
				placeLamp(world, box, rand, 6, 4, 6);
				
				if(bulkheadNZ) {
					fillWithBlocks(world, box, 1, 1, 0, 2, 1, 0, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 1, 2, 0, 2, 2, 0, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 1, 3, 0, 2, 3, 0, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 6, 1, 0, 7, 1, 0, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 6, 2, 0, 7, 2, 0, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 6, 3, 0, 7, 3, 0, ModBlocks.reinforced_brick);
					fillWithAir(world, box, 3, 1, 0, 5, 3, 0);
				} else
					fillWithAir(world, box, 1, 1, 0, 7, 3, 0);
				
				if(opensPZ) {
					fillWithBlocks(world, box, 1, 0, 7, 1, 0, end, ModBlocks.deco_titanium); //Floor
					fillWithBlocks(world, box, 2, 0, 7, 2, 0, end, ModBlocks.tile_lab);
					fillWithBlocks(world, box, 3, 0, 7, 5, 0, 8, ModBlocks.deco_titanium);
					fillWithBlocks(world, box, 6, 0, 7, 6, 0, end, ModBlocks.tile_lab);
					fillWithBlocks(world, box, 7, 0, 7, 7, 0, end, ModBlocks.deco_titanium);
					fillWithBlocks(world, box, 1, 4, 7, 1, 4, end, ModBlocks.reinforced_brick); //Ceiling
					fillWithMetadataBlocks(world, box, 2, 4, 7, 2, 4, end, ModBlocks.concrete_pillar, pillarMetaNS);
					fillWithBlocks(world, box, 3, 4, 5, 3, 4, 8, ModBlocks.reinforced_brick);
					fillWithMetadataBlocks(world, box, 4, 4, 5, 4, 4, 8, ModBlocks.concrete_pillar, pillarMetaNS);
					fillWithBlocks(world, box, 5, 4, 5, 5, 4, 8, ModBlocks.reinforced_brick);
					fillWithMetadataBlocks(world, box, 6, 4, 7, 6, 4, end, ModBlocks.concrete_pillar, pillarMetaNS);
					fillWithBlocks(world, box, 7, 4, 7, 7, 4, end, ModBlocks.reinforced_brick);
					
					if(bulkheadPZ) {
						fillWithBlocks(world, box, 1, 1, 8, 2, 1, 8, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 1, 2, 8, 2, 2, 8, ModBlocks.reinforced_stone);
						fillWithBlocks(world, box, 1, 3, 8, 2, 3, 8, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 6, 1, 8, 7, 1, 8, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 6, 2, 8, 7, 2, 8, ModBlocks.reinforced_stone);
						fillWithBlocks(world, box, 6, 3, 8, 7, 3, 8, ModBlocks.reinforced_brick);
						fillWithAir(world, box, 3, 1, 8, 5, 3, 8);
					} else
						fillWithAir(world, box, 1, 1, 8, 7, 3, 8);
				} else {
					fillWithBlocks(world, box, 1, 0, 7, 7, 0, 7, ModBlocks.deco_titanium); //Floor
					fillWithBlocks(world, box, 1, 1, 8, 7, 1, 8, ModBlocks.reinforced_brick); //Wall
					fillWithBlocks(world, box, 1, 2, 8, 7, 2, 8, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 1, 3, 8, 7, 3, 8, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 3, 4, 5, 5, 4, 6, ModBlocks.reinforced_brick); //Ceiling
					fillWithBlocks(world, box, 1, 4, 7, 7, 4, 7, ModBlocks.reinforced_brick);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 4, 4, 5, box);
				}
				
				if(opensNX) {
					fillWithBlocks(world, box, 1, 0, start, 1, 0, 1, ModBlocks.deco_titanium); //Floor
					
					fillWithBlocks(world, box, right, 0, 2, 1, 0, 2, ModBlocks.tile_lab);
					fillWithBlocks(world, box, 0, 0, 3, 1, 0, 5, ModBlocks.deco_titanium);
					fillWithBlocks(world, box, right, 0, 6, 1, 0, 6, ModBlocks.tile_lab);
					
					 //Ceiling
					fillWithMetadataBlocks(world, box, right, 4, 2, 1, 4, 2, ModBlocks.concrete_pillar, pillarMetaWE);
					fillWithBlocks(world, box, 0, 4, 3, 2, 4, 3, ModBlocks.reinforced_brick);
					fillWithMetadataBlocks(world, box, 0, 4, 4, 3, 4, 4, ModBlocks.concrete_pillar, pillarMetaWE);
					fillWithBlocks(world, box, 0, 4, 5, 2, 4, 5, ModBlocks.reinforced_brick);
					fillWithMetadataBlocks(world, box, right, 4, 6, 1, 4, 6, ModBlocks.concrete_pillar, pillarMetaWE);
					
					if(bulkheadNX) {
						fillWithBlocks(world, box, 0, 1, 1, 0, 1, 2, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 0, 2, 1, 0, 2, 2, ModBlocks.reinforced_stone);
						fillWithBlocks(world, box, 0, 3, 1, 0, 3, 2, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 0, 1, 6, 0, 1, 7, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 0, 2, 6, 0, 2, 7, ModBlocks.reinforced_stone);
						fillWithBlocks(world, box, 0, 3, 6, 0, 3, 7, ModBlocks.reinforced_brick);
						fillWithAir(world, box, 0, 1, 3, 0, 3, 5);
					} else {
						fillWithAir(world, box, 0, 1, 1, 0, 3, 7);
						placeBlockAtCurrentPosition(world, ModBlocks.deco_titanium, 0, 0, 0, 1, box); //outlier single-block placing operations
						placeBlockAtCurrentPosition(world, ModBlocks.deco_titanium, 0, 0, 0, 7, box);
						placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 4, 1, box);
						placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 4, 7, box);
					}
				} else {
					fillWithBlocks(world, box, 1, 0, start, 1, 0, 6, ModBlocks.deco_titanium); //Floor
					fillWithBlocks(world, box, 0, 1, 1, 0, 1, 7, ModBlocks.reinforced_brick); //Wall
					fillWithBlocks(world, box, 0, 2, 1, 0, 2, 7, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 0, 3, 1, 0, 3, 7, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 1, 4, 2, 1, 4, 6, ModBlocks.reinforced_brick); //Ceiling
					fillWithBlocks(world, box, 2, 4, 3, 2, 4, 5, ModBlocks.reinforced_brick);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 3, 4, 4, box);
				}
				
				if(opensPX) {
					 //Floor
					fillWithBlocks(world, box, 7, 0, start, 7, 0, 1, ModBlocks.deco_titanium);
					fillWithBlocks(world, box, 7, 0, 2, left, 0, 2, ModBlocks.tile_lab);
					fillWithBlocks(world, box, 7, 0, 3, 8, 0, 5, ModBlocks.deco_titanium);
					fillWithBlocks(world, box, 7, 0, 6, left, 0, 6, ModBlocks.tile_lab);
					 //Ceiling
					fillWithMetadataBlocks(world, box, 7, 4, 2, left, 4, 2, ModBlocks.concrete_pillar, pillarMetaWE);
					fillWithBlocks(world, box, 6, 4, 3, 8, 4, 3, ModBlocks.reinforced_brick);
					fillWithMetadataBlocks(world, box, 5, 4, 4, 8, 4, 4, ModBlocks.concrete_pillar, pillarMetaWE);
					fillWithBlocks(world, box, 6, 4, 5, 8, 4, 5, ModBlocks.reinforced_brick);
					fillWithMetadataBlocks(world, box, 7, 4, 6, left, 4, 6, ModBlocks.concrete_pillar, pillarMetaWE);
					
					if(bulkheadPX) {
						fillWithBlocks(world, box, 8, 1, 1, 8, 1, 2, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 8, 2, 1, 8, 2, 2, ModBlocks.reinforced_stone);
						fillWithBlocks(world, box, 8, 3, 1, 8, 3, 2, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 8, 1, 6, 8, 1, 7, ModBlocks.reinforced_brick);
						fillWithBlocks(world, box, 8, 2, 6, 8, 2, 7, ModBlocks.reinforced_stone);
						fillWithBlocks(world, box, 8, 3, 6, 8, 3, 7, ModBlocks.reinforced_brick);
						fillWithAir(world, box, 8, 1, 3, 8, 3, 5);
					} else {
						fillWithAir(world, box, 8, 1, 1, 8, 3, 7);
						placeBlockAtCurrentPosition(world, ModBlocks.deco_titanium, 0, 8, 0, 1, box);
						placeBlockAtCurrentPosition(world, ModBlocks.deco_titanium, 0, 8, 0, 7, box);
						placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 8, 4, 1, box);
						placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 8, 4, 7, box);
					}
				} else {
					fillWithBlocks(world, box, 7, 0, start, 7, 0, 6, ModBlocks.deco_titanium); //Floor
					fillWithBlocks(world, box, 8, 1, 1, 8, 1, 7, ModBlocks.reinforced_brick); //Wall
					fillWithBlocks(world, box, 8, 2, 1, 8, 2, 7, ModBlocks.reinforced_stone);
					fillWithBlocks(world, box, 8, 3, 1, 8, 3, 7, ModBlocks.reinforced_brick);
					fillWithBlocks(world, box, 6, 4, 3, 6, 4, 5, ModBlocks.reinforced_brick); //Ceiling
					fillWithBlocks(world, box, 7, 4, 2, 7, 4, 6, ModBlocks.reinforced_brick);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 5, 4, 4, box);
				}
				//Wall corners
				if(opensNX || opensPZ) {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 1, 8, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 0, 2, 8, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 0, 3, 8, box);
				}
				
				if(opensPX || opensPZ) {
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 8, 1, 8, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_stone, 0, 8, 2, 8, box);
					placeBlockAtCurrentPosition(world, ModBlocks.reinforced_brick, 0, 8, 3, 8, box);
				}
				
				if(underwater) {
					fillWithWater(world, box, rand, 1, 1, 0, 7, 3, opensPZ ? 8 : 7, 1);
					if(opensNX) fillWithWater(world, box, rand, 0, 1, bulkheadNX ? 3 : 1, 0, 3, bulkheadNX ? 5 : 7, 1);
					if(opensPX) fillWithWater(world, box, rand, 8, 1, bulkheadPX ? 3 : 1, 8, 3, bulkheadPX ? 5 : 7, 1);
				} else
					fillWithCobwebs(world, box, rand, opensNX ? 0 : 1, 1, 0, opensPX ? 8 : 7, 3, opensPZ ? 8 : 7);
				
				return true;
			}
		}
	}
}