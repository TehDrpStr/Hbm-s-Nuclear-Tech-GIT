package com.hbm.handler.guncfg;

import java.util.ArrayList;
import java.util.List;

import com.hbm.entity.projectile.EntityBulletBase;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNT.ExAttrib;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.handler.BulletConfigSyncingUtil;
import com.hbm.handler.BulletConfiguration;
import com.hbm.handler.GunConfiguration;
import com.hbm.interfaces.IBulletHurtBehavior;
import com.hbm.interfaces.IBulletImpactBehavior;
import com.hbm.interfaces.IBulletUpdateBehavior;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ModItems;
import com.hbm.items.ItemAmmoEnums.Ammo4Gauge;
import com.hbm.lib.HbmCollection;
import com.hbm.lib.HbmCollection.EnumGunManufacturer;
import com.hbm.lib.ModDamageSource;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.PacketDispatcher;
import com.hbm.potion.HbmPotion;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations.AnimType;
import com.hbm.render.util.RenderScreenOverlay.Crosshair;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.IExtendedEntityProperties;

public class Gun4GaugeFactory {
	
	private static GunConfiguration getShotgunConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 15;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.firingDuration = 0;
		config.ammoCap = 4;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.hasSights = true;
		config.crosshair = Crosshair.L_CIRCLE;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		
		return config;
	}
	
	public static GunConfiguration getKS23Config() {
		
		GunConfiguration config = getShotgunConfig();
		
		config.durability = 3000;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.firingSound = "hbm:weapon.revolverShootAlt";
		config.firingPitch = 0.65F;
		
		config.name = "ks23";
		config.manufacturer = EnumGunManufacturer.TULSKY;

		config.config = HbmCollection.fourGauge;
		
		return config;
	}
	
	public static GunConfiguration getSauerConfig() {
		
		GunConfiguration config = getShotgunConfig();

		config.rateOfFire = 20;
		config.ammoCap = 0;
		config.reloadType = GunConfiguration.RELOAD_NONE;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.durability = 3000;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.firingSound = "hbm:weapon.sauergun";
		config.firingPitch = 1.0F;
		
		config.name = "sauer";
		config.manufacturer = EnumGunManufacturer.CUBE;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("SAUER_RECOIL", new BusAnimationSequence()
						.addKeyframe(new BusAnimationKeyframe(0.5, 0, 0, 50))
						.addKeyframe(new BusAnimationKeyframe(0, 0, 0, 50))
						)
				.addBus("SAUER_TILT", new BusAnimationSequence()
						.addKeyframe(new BusAnimationKeyframe(0.0, 0, 0, 200))	// do nothing for 200ms
						.addKeyframe(new BusAnimationKeyframe(0, 0, 30, 150))	//tilt forward
						.addKeyframe(new BusAnimationKeyframe(45, 0, 30, 150))	//tilt sideways
						.addKeyframe(new BusAnimationKeyframe(45, 0, 30, 200))	//do nothing for 200ms (eject)
						.addKeyframe(new BusAnimationKeyframe(0, 0, 30, 150))	//restore sideways
						.addKeyframe(new BusAnimationKeyframe(0, 0, 0, 150))	//restore forward
						)
				.addBus("SAUER_COCK", new BusAnimationSequence()
						.addKeyframe(new BusAnimationKeyframe(0, 0, 0, 500))	//do nothing for 500ms
						.addKeyframe(new BusAnimationKeyframe(1, 0, 0, 100))	//pull back lever for 100ms
						.addKeyframe(new BusAnimationKeyframe(0, 0, 0, 100))	//release lever for 100ms
						)
				.addBus("SAUER_SHELL_EJECT", new BusAnimationSequence()
						.addKeyframe(new BusAnimationKeyframe(0, 0, 0, 500))	//do nothing for 500ms
						.addKeyframe(new BusAnimationKeyframe(0, 0, 1, 500))	//FLING!
						)
				);
		
		config.config = HbmCollection.fourGauge;
		
		return config;
	}
	
	public static BulletConfiguration get4GaugeConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.STOCK));
		bullet.dmgMin = 5;
		bullet.dmgMax = 8;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		
		return bullet;
	}
	
	public static BulletConfiguration get4GaugeSlugConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.SLUG));
		bullet.dmgMin = 25;
		bullet.dmgMax = 32;
		bullet.wear = 7;
		bullet.style = BulletConfiguration.STYLE_NORMAL;
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeFlechetteConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.FLECHETTE));
		bullet.dmgMin = 8;
		bullet.dmgMax = 15;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.wear = 15;
		bullet.style = BulletConfiguration.STYLE_FLECHETTE;
		bullet.HBRC = 2;
		bullet.LBRC = 95;
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeFlechettePhosphorusConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.FLECHETTE_PHOSPHORUS));
		bullet.dmgMin = 8;
		bullet.dmgMax = 15;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.wear = 15;
		bullet.style = BulletConfiguration.STYLE_FLECHETTE;
		bullet.HBRC = 2;
		bullet.LBRC = 95;
		bullet.incendiary = 5;
		
		PotionEffect eff = new PotionEffect(HbmPotion.phosphorus.id, 20 * 20, 0, true);
		eff.getCurativeItems().clear();
		bullet.effects = new ArrayList();
		bullet.effects.add(new PotionEffect(eff));
		
		bullet.bImpact = new IBulletImpactBehavior() {

			@Override
			public void behaveBlockHit(EntityBulletBase bullet, int x, int y, int z) {
				
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "vanillaburst");
				data.setString("mode", "flame");
				data.setInteger("count", 15);
				data.setDouble("motion", 0.05D);
				
				PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(data, bullet.posX, bullet.posY, bullet.posZ), new TargetPoint(bullet.dimension, bullet.posX, bullet.posY, bullet.posZ, 50));
			}
		};
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeExplosiveConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.EXPLOSIVE));
		bullet.velocity *= 2;
		bullet.gravity *= 2;
		bullet.dmgMin = 20;
		bullet.dmgMax = 25;
		bullet.wear = 25;
		bullet.trail = 1;
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeMiningConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.MINING));
		bullet.velocity *= 2;
		bullet.gravity *= 2;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 25;
		bullet.trail = 1;
		bullet.explosive = 0.0F;
		
		bullet.bImpact = new IBulletImpactBehavior() {

			@Override
			public void behaveBlockHit(EntityBulletBase bullet, int x, int y, int z) {
				
				if(bullet.worldObj.isRemote)
					return;
				
				ExplosionNT explosion = new ExplosionNT(bullet.worldObj, null, bullet.posX, bullet.posY, bullet.posZ, 4);
				explosion.atttributes.add(ExAttrib.ALLDROP);
				explosion.atttributes.add(ExAttrib.NOHURT);
				explosion.doExplosionA();
				explosion.doExplosionB(false);
				
				ExplosionLarge.spawnParticles(bullet.worldObj, bullet.posX, bullet.posY, bullet.posZ, 15);
			}
		};
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeBalefireConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.BALEFIRE));
		bullet.velocity *= 2;
		bullet.gravity *= 2;
		bullet.dmgMin = 50;
		bullet.dmgMax = 65;
		bullet.wear = 25;
		bullet.trail = 1;
		bullet.explosive = 0.0F;
		
		bullet.bImpact = new IBulletImpactBehavior() {

			@Override
			public void behaveBlockHit(EntityBulletBase bullet, int x, int y, int z) {
				
				if(bullet.worldObj.isRemote)
					return;
				
				ExplosionNT explosion = new ExplosionNT(bullet.worldObj, null, bullet.posX, bullet.posY, bullet.posZ, 6);
				explosion.atttributes.add(ExAttrib.BALEFIRE);
				explosion.doExplosionA();
				explosion.doExplosionB(false);
				
				ExplosionLarge.spawnParticles(bullet.worldObj, bullet.posX, bullet.posY, bullet.posZ, 30);
			}
		};
		
		return bullet;
	}

	public static BulletConfiguration getGrenadeKampfConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.KAMPF));
		bullet.spread = 0.0F;
		bullet.gravity = 0.0D;
		bullet.wear = 15;
		bullet.explosive = 3.5F;
		bullet.style = BulletConfiguration.STYLE_GRENADE;
		bullet.trail = 4;
		bullet.vPFX = "smoke";
		
		return bullet;
	}

	public static BulletConfiguration getGrenadeCanisterConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.CANISTER));
		bullet.spread = 0.0F;
		bullet.gravity = 0.0D;
		bullet.wear = 15;
		bullet.explosive = 1F;
		bullet.style = BulletConfiguration.STYLE_GRENADE;
		bullet.trail = 4;
		bullet.vPFX = "smoke";
		
		bullet.bUpdate = new IBulletUpdateBehavior() {

			@Override
			public void behaveUpdate(EntityBulletBase bullet) {
				
				if(!bullet.worldObj.isRemote) {
					
					if(bullet.ticksExisted > 10) {
						bullet.setDead();
						
						for(int i = 0; i < 50; i++) {
							
							EntityBulletBase bolt = new EntityBulletBase(bullet.worldObj, BulletConfigSyncingUtil.M44_AP);
							bolt.setPosition(bullet.posX, bullet.posY, bullet.posZ);
							bolt.setThrowableHeading(bullet.motionX, bullet.motionY, bullet.motionZ, 0.25F, 0.1F);
							bullet.worldObj.spawnEntityInWorld(bolt);
						}
					}
				}
			}
		};
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeSleekConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardAirstrikeConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.SLEEK));
		
		return bullet;
	}
	
	public static BulletConfiguration get4GaugeClawConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.CLAW));
		bullet.dmgMin = 6;
		bullet.dmgMax = 9;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.leadChance = 100;
		
		bullet.bHurt = new IBulletHurtBehavior() {

			@Override
			public void behaveEntityHurt(EntityBulletBase bullet, Entity hit) {
				
				if(bullet.worldObj.isRemote)
					return;
				
				if(hit instanceof EntityLivingBase) {
					EntityLivingBase living = (EntityLivingBase) hit;
					float f = living.getHealth();
					
					if(f > 0) {
						f = Math.max(0, f - 2);
						living.setHealth(f);
						
						if(f == 0)
							living.onDeath(ModDamageSource.causeBulletDamage(bullet, hit));
					}
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration get4GaugeVampireConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.VAMPIRE));
		bullet.dmgMin = 6;
		bullet.dmgMax = 9;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.leadChance = 100;
		bullet.style = BulletConfiguration.STYLE_FLECHETTE;
		
		bullet.bHurt = new IBulletHurtBehavior() {

			@Override
			public void behaveEntityHurt(EntityBulletBase bullet, Entity hit) {
				
				if(bullet.worldObj.isRemote)
					return;
				
				if(hit instanceof EntityPlayer) {
					EntityPlayer player = (EntityPlayer) hit;
					
					IExtendedEntityProperties prop = player.getExtendedProperties("WitcheryExtendedPlayer");
					
					NBTTagCompound blank = new NBTTagCompound();
					blank.setTag("WitcheryExtendedPlayer", new NBTTagCompound());
					
					if(prop != null) {
						prop.loadNBTData(blank);
					}
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration get4GaugeVoidConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.VOID));
		bullet.dmgMin = 6;
		bullet.dmgMax = 9;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.leadChance = 0;
		
		bullet.bHurt = new IBulletHurtBehavior() {

			@Override
			public void behaveEntityHurt(EntityBulletBase bullet, Entity hit) {
				
				if(bullet.worldObj.isRemote)
					return;
				
				if(hit instanceof EntityPlayer) {
					EntityPlayer player = (EntityPlayer) hit;
					
					player.inventory.dropAllItems();
					player.worldObj.newExplosion(bullet.shooter, player.posX, player.posY, player.posZ, 5.0F, true, true);
				}
			}
		};
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeQuackConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = new ComparableStack(ModItems.ammo_4gauge.stackFromEnum(Ammo4Gauge.QUACK));
		bullet.velocity *= 2D;
		bullet.spread = 0.0F;
		bullet.gravity = 0.0D;
		bullet.wear = 10;
		bullet.explosive = 1F;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = 4;
		bullet.vPFX = "explode";
		
		bullet.bUpdate = new IBulletUpdateBehavior() {

			@Override
			public void behaveUpdate(EntityBulletBase bullet) {
				
				if(!bullet.worldObj.isRemote) {
					
					if(bullet.ticksExisted % 2 == 0) {
						
						List<EntityCreature> creatures = bullet.worldObj.getEntitiesWithinAABB(EntityCreature.class, bullet.boundingBox.expand(10, 10, 10));
						
						for(EntityCreature creature : creatures) {
							
							if(creature.getClass().getCanonicalName().startsWith("net.minecraft.entity.titan")) {
								ExplosionNukeSmall.explode(bullet.worldObj, creature.posX, creature.posY, creature.posZ, ExplosionNukeSmall.medium);

								bullet.worldObj.removeEntity(creature);
								bullet.worldObj.unloadEntities(new ArrayList() {{ add(creature); }});
								//creature.isDead = true;
								
								/*try {
									Method m = Class.forName("net.minecraft.entity.deity.EntityDeity").getDeclaredMethod("setTitanHealth", double.class);
									m.setAccessible(true);
									m.invoke(creature, 0.0D);
								} catch (Exception ex) { }*/
							}
						}
						
					}
				}
			}
		};
		
		return bullet;
	}
}
