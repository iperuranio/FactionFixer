package FactionFixer;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Relation;

public class Main extends JavaPlugin implements Listener
{
//	ArrayList<Player> inCombat = new ArrayList<Player>();
//	List<String> allow = Arrays.asList("/warp war");
//	
	
	public static HashMap<Player, Integer> clicks = new HashMap<Player, Integer>();
private static Main m = null;
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
		m = this;
		removeClicks();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPickpu(PlayerPickupItemEvent e) {
		if(e.isCancelled()) 
			return;
		
		e.setCancelled(true);
		
		final Player p = e.getPlayer();
		final ItemStack is = e.getItem().getItemStack();
		e.getItem().remove();
		
		if(is != null) {
			HashMap<Integer, ItemStack> items = p.getInventory().addItem(is);
			p.playSound(p.getLocation(), Sound.SHEEP_IDLE, 5, 1);
			
			if(items.isEmpty())
				return;
			
			final Location l = p.getLocation();
			final World w = l.getWorld();
			
			items.forEach((a, b) -> w.dropItemNaturally(l, b));
		}
	}
	
	public boolean isCommand(String s) {
		return s.startsWith("/warp war") || s.startsWith("/eff");
	}
	
	@EventHandler
	public void command(PlayerCommandPreprocessEvent e)
	{
		String m = e.getMessage();
		
		Faction commandExecuter = FPlayers.getInstance().getByPlayer(e.getPlayer()).getFaction();
		Faction territory = Board.getInstance().getFactionAt(new FLocation(e.getPlayer().getLocation()));
		
		if(commandExecuter.getRelationWish(territory) == Relation.ENEMY && !isCommand(m)) {
			e.setCancelled(true);
			e.getPlayer().sendMessage("§cNon puoi eseguire questo comando in territorio nemico.");
		}
	}
	
	public static Main get() {
		return m ;
	}
	
	public static void removeClicks() 
	{
		Bukkit.getServer().getScheduler().runTaskTimer(Main.get(), new Runnable() 
		{
			public void run() 
			{
				for (Entry<Player, Integer> entry : Main.clicks.entrySet())
				{
					if (entry.getValue() >= 150)
					{
						Main.notifyToStaff("§c§lControllo §8§l» §c"+ entry.getKey().getName()+" §7ha una media di §c"+ entry.getValue()/10+ " §7click/s.");
					}
				}
				Main.clicks.clear();
			}
		}, 20L, 200L);
	}
	
	private static void notifyToStaff(String string) {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.hasPermission("controllo.cps")) {
				p.sendMessage(string);
			}
		}
	}
	
	@EventHandler
	public void click(PlayerInteractEvent e) {
		
		final Player p = e.getPlayer();
		
		if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) 
		{
			if (clicks.get(p) == null)
			{
				clicks.put(p, 1);
			}
			else
			{
				clicks.put(p, clicks.get(p) + 1);
			}
        }
	}
	
//	@EventHandler
//    public void onPlayerTagEvent(PlayerTagEvent e) 
//	{
//		if (!inCombat.contains(e.getDamagee()))
//			inCombat.add(e.getDamagee());
//		if (!inCombat.contains(e.getDamager()))
//			inCombat.add(e.getDamager());
//    }
//    
//    @EventHandler
//    public void onPlayerUntagEvent(PlayerUntagEvent e) 
//    {
//    	inCombat.remove(e.getPlayer());
//    }
}
