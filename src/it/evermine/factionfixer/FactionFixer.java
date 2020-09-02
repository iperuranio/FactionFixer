package it.evermine.factionfixer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Relation;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.iiSnipez.CombatLog.Events.PlayerTagEvent;

public class FactionFixer extends JavaPlugin implements Listener {
	ArrayList<String> inCombat = new ArrayList<String>();
//	List<String> allow = Arrays.asList("/warp war");
//	86

	public final static Location l = new Location(Bukkit.getWorld("world"), 0, 89, 0);
	public static HashMap<Player, Integer> clicks = new HashMap<Player, Integer>();
	private static FactionFixer m = null;
	public WorldGuardPlugin worldGuardAPI = null;
	public static HashMap<String, Long> fact = new HashMap<>();

	final int minutes = 15;

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		m = this;
		removeClicks();

		if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
			worldGuardAPI = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		if (inCombat.contains(e.getPlayer().getName()) && e.getTo().distance(l) < 50) {
			final Location loc = e.getTo();
			worldGuardAPI.getRegionContainer().get(loc.getWorld())
					.getApplicableRegionsIDs(new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
					.forEach(s -> {});
		}
	}

	@EventHandler
	public void onTP(PlayerTeleportEvent e) {
		if (e.isCancelled())
			return;

		if (e.getCause() == TeleportCause.ENDER_PEARL || e.getCause() == TeleportCause.UNKNOWN)
			return;

		if (e.getPlayer().isOp()) {
			return;
		}

		Faction tpExecuter = FPlayers.getInstance().getByPlayer(e.getPlayer()).getFaction();
		Faction territory = Board.getInstance().getFactionAt(new FLocation(e.getTo()));

		if (tpExecuter.getRelationWish(territory) == Relation.ENEMY) {
			e.setCancelled(true);
			e.getPlayer().sendMessage("§cNon puoi essere teletrasportato in un territorio nemico.");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPickpu(PlayerPickupItemEvent e) {
		if (e.isCancelled())
			return;

		e.setCancelled(true);

		final Player p = e.getPlayer();
		final ItemStack is = e.getItem().getItemStack();
		e.getItem().remove();

		if (is != null) {
			HashMap<Integer, ItemStack> items = p.getInventory().addItem(is);
			p.playSound(p.getLocation(), Sound.SHEEP_IDLE, 5, 1);

			if (items.isEmpty())
				return;

			final Location l = p.getLocation();
			final World w = l.getWorld();

			items.forEach((a, b) -> w.dropItemNaturally(l, b));
		}
	}

	public boolean isCommand(String s) {
		return s.startsWith("/warp war") || s.startsWith("/eff") || s.startsWith("/fly");
	}

	@EventHandler
	public void command(PlayerCommandPreprocessEvent e) {
		if (e.getPlayer().isOp()) {
			return;
		}

		String m = e.getMessage();

		if (m.startsWith("/f ") && (m.startsWith("/f raid ") || m.startsWith("/f raid"))) {
			Faction commandExecuter = FPlayers.getInstance().getByPlayer(e.getPlayer()).getFaction();
			String id = commandExecuter.getId();

			if (fact.containsKey(id)) {
				e.getPlayer().sendMessage("§2§lFazioni §8§l» §7Il RAID durerà ancora §a" + longToTime(fact.get(id)));
			} else {
				e.getPlayer().sendMessage("§2§lFazioni §8§l» §7La tua fazione §cnon §aè sotto RAID.");
			}

			return;
		}

		Faction commandExecuter = FPlayers.getInstance().getByPlayer(e.getPlayer()).getFaction();
		Faction territory = Board.getInstance().getFactionAt(new FLocation(e.getPlayer().getLocation()));

		if (commandExecuter.getRelationWish(territory) == Relation.ENEMY && !isCommand(m)) {
			e.setCancelled(true);
			e.getPlayer().sendMessage("§cNon puoi eseguire questo comando in territorio nemico.");
		}
	}

	public static FactionFixer get() {
		return m;
	}

	public static void removeClicks() {
		Bukkit.getServer().getScheduler().runTaskTimer(FactionFixer.get(), new Runnable() {
			@Override
			public void run() {
				for (Entry<Player, Integer> entry : FactionFixer.clicks.entrySet()) {
					if (entry.getValue() >= 150) {
						FactionFixer.notifyToStaff("§c§lControllo §8§l» §c" + entry.getKey().getName() + " §7ha una media di §c"
								+ entry.getValue() / 10 + " §7click/s.");
					}
				}
				FactionFixer.clicks.clear();
			}
		}, 20L, 200L);
	}

	private static void notifyToStaff(String string) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.hasPermission("controllo.cps")) {
				p.sendMessage(string);
			}
		}
	}

	public static String longToTime(long tempo) {
		long[] numeri = new long[4];
		String[] singolare = { "giorno", "ora", "minuto", "secondo" };
		String[] plurale = { "giorni", "ore", "minuti", "secondi" };
		numeri[0] = (long) (tempo / 1728000);
		tempo -= 1728000 * numeri[0];
		numeri[1] = (long) (tempo / 72000);
		tempo -= 72000 * numeri[1];
		numeri[2] = (long) (tempo / 1200);
		tempo -= 1200 * numeri[2];
		numeri[3] = (long) (tempo / 20);
		StringBuilder phraseBuilder = new StringBuilder("");
		byte numberOfComma = -2;
		for (long numero : numeri) {
			if (numero != 0) {
				numberOfComma++;
			}
		}
		for (int k = 0; k < numeri.length; k++) {
			if (numeri[k] == 0) {
				continue;
			}
			boolean isSingular = numeri[k] == 1;
			phraseBuilder.append(String.valueOf(numeri[k]) + " " + (isSingular ? singolare[k] : plurale[k]));
			if (numberOfComma == 0) {
				phraseBuilder.append(" e ");
			}
			if (numberOfComma > 0) {
				phraseBuilder.append(", ");
			}
			numberOfComma--;
		}
		return phraseBuilder.toString();
	}
//	
//	@EventHandler
//	public void onSpawn(CreatureSpawnEvent e) {
//		if(e.isCancelled())
//			return;
//		
//		if(e.getEntityType() == EntityType.CREEPER) {
//			if(e.getSpawnReason().toString().toLowerCase().contains("egg")) {
//				Creeper cr = (Creeper) e.getEntity();
//				creeper.put(cr.getUniqueId(), );
//			}
//		}
//	}

	@EventHandler
	public void onExplosion(EntityExplodeEvent e) {
		if (e.getEntity() instanceof TNTPrimed) {
			TNTPrimed tnt = (TNTPrimed) e.getEntity();
			Entity source = tnt.getSource();

			if (source != null && source instanceof Player) {
				Player p = (Player) source;

				Faction tntPlacer = FPlayers.getInstance().getByPlayer(p).getFaction();
				Faction territory = Board.getInstance().getFactionAt(new FLocation(e.getLocation()));

				if (territory.isWarZone() || territory.isWilderness()) {
					return;
				}

				if (fact.containsKey(territory.getId())) {
					fact.put(territory.getId(), (long) (minutes * 60 * 20));
					return;
				}

				if (tntPlacer.getRelationWish(territory) == Relation.ENEMY) {
					territory.sendMessage("§c§lLa fazione §4" + tntPlacer.getTag()
							+ " §c§lvi ha attaccato! Siete sotto RAID!\n§c§lNON distruggete gli Spawner.§c§lIl RAID terminerà dopo §4"
							+ minutes + " §c§l dall'ultima TNT esplosa.\n§c§lVerifica lo stato con §4/f raid§c§l.");
					territory.getOnlinePlayers().forEach(pl -> {
						pl.playSound(pl.getLocation(), Sound.ENDERDRAGON_GROWL, 10, 1);
					});
					tntPlacer.sendMessage(
							"§c§lLa tua fazione ha iniziato un RAID contro la fazione §4" + territory.getTag());

					final String id = territory.getId();

					this.getServer().getScheduler().runTaskTimer(this, new Runnable() {
						@Override
						public void run() {
							if (update(id)) {
								territory.sendMessage("§a§lRAID contro §2" + territory.getTag() + "§a§l TERMINATO.");
								territory.sendMessage(
										"§a§lRAID iniziato da §2" + tntPlacer.getTag() + "§a§l TERMINATO.");
								fact.remove(id);
							}
						}
					}, 0L, 20L);
				}
			}
		} // else if(e.getEntity() instanceof Creeper) {
//			Creeper creeper = (Creeper) e.getEntity();
//			Entity source = creeper.
		// }
	}

	public boolean update(String s) {
		Long l = fact.get(s);
		if (l == null)
			return false;

		if (l <= 20) {
			return true;
		}

		fact.put(s, l - 20);
		return false;
	}

	@EventHandler
	public void click(PlayerInteractEvent e) {

		final Player p = e.getPlayer();

		if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) {
			if (clicks.get(p) == null) {
				clicks.put(p, 1);
			} else {
				clicks.put(p, clicks.get(p) + 1);
			}
		}
	}

	@EventHandler
	public void enchant(EnchantItemEvent e) {
		e.setCancelled(true);
		e.getEnchanter().closeInventory();
		e.getEnchanter().sendMessage("§cL'enchanting è disabilitato.");
	}

	@EventHandler
	public void enchant(BrewEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	public void interact(PlayerInteractEntityEvent e) {
		if (e.getRightClicked().getType().equals(EntityType.VILLAGER)
				&& e.getRightClicked().getLocation().getWorld().getName().equals("Spawn")) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void joinTag(PlayerTagEvent e) {
		if (inCombat.contains(e.getDamager().getName()))
			inCombat.add(e.getDamager().getName());

		if (inCombat.contains(e.getDamagee().getName()))
			inCombat.add(e.getDamagee().getName());
	}

	@EventHandler
	public void leftTag(me.iiSnipez.CombatLog.Events.PlayerUntagEvent e) {
		inCombat.remove(e.getPlayer().getName());
	}
}
