import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public final class GameLoop {
    private final ServerContext context;
    private final Timer timer;
    private final Random random = new Random();
    private long tickCount = 0;

    public GameLoop(ServerContext context) {
        this.context = context;
        this.timer = new Timer("GameLoop", true);
    }

    public void start() {
        long interval = context.getConfig().getTickInterval().toMillis();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    tick();
                } catch (Exception ex) {
                    Logger.error("GameLoop tick failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }, interval, interval);
        Logger.info("GameLoop started (interval=" + interval + "ms)");
    }

    public void stop() {
        timer.cancel();
        Logger.info("GameLoop stopped");
    }

    private void tick() throws Exception {
        tickCount++;
        Logger.debug("=== TICK " + tickCount + " START ===");

        processNpcs();
        processCombat();
        processPlayers();

        Logger.debug("=== TICK " + tickCount + " END ===");
    }

    /**
     * Process all NPC AI: random walking within bounds, respawn timers.
     */
    private void processNpcs() {
        List<Npc> npcs = context.getWorldService().getNpcs();
        for (Npc npc : npcs) {
            // Handle respawn timers for dead NPCs
            if (npc.isDead()) {
                npc.decrementRespawnTimer();
                if (npc.getRespawnTimer() <= 0) {
                    npc.respawn();
                }
                continue; // Dead NPCs don't walk
            }

            // Only walk if not in combat
            if (!npc.isInCombat()) {
                npc.tryRandomWalk();
            }
        }
    }

    /**
     * Process all active combat between players and NPCs.
     * Uses a 3-tick combat cycle matching RSC mechanics.
     */
    private void processCombat() throws Exception {
        List<Player> snapshot = new ArrayList<>(context.getPlayers().getOnlinePlayers());
        WorldService world = context.getWorldService();

        for (Player player : snapshot) {
            if (player == null || !player.isInCombat()) continue;

            Npc npc = player.getAttackingNpc();
            if (npc == null || npc.isDead()) {
                endCombat(player, npc);
                continue;
            }

            // Decrement combat timer
            player.decrementCombatTimer();
            if (player.getCombatTimer() > 0) continue;

            // Reset timer for next round (3 ticks = ~1.9 seconds)
            player.setCombatTimer(3);
            npc.setCombatTimer(3);

            NpcDefinition npcDef = world.getNpcDefinition(npc.getTypeId());
            if (npcDef == null) {
                endCombat(player, npc);
                continue;
            }

            // Player attacks NPC
            int playerDamage = rollPlayerDamage(player, npcDef);
            int actualDamage = npc.takeDamage(playerDamage);

            // Send NPC damage update to all nearby players
            int radius = context.getConfig().getVisibilityRadius();
            List<Player> nearbyPlayers = new ArrayList<>(context.getPlayers().getOnlinePlayers());
            for (Player viewer : nearbyPlayers) {
                if (viewer != null && Math.abs(viewer.getX() - npc.getX()) <= radius
                        && Math.abs(viewer.getY() - npc.getY()) <= radius) {
                    try {
                        PlayerPacketSender.sendNpcDamage(viewer, npc, actualDamage,
                                npc.getCurrentHits(), npc.getMaxHits());
                    } catch (Exception ex) {
                        Logger.error("Failed to send NPC damage to " + viewer.getUsername());
                    }
                }
            }

            Logger.debug("Combat: " + player.getUsername() + " hits " + npcDef.getName() +
                         " for " + actualDamage + " (HP: " + npc.getCurrentHits() + "/" + npc.getMaxHits() + ")");

            // Check if NPC died
            if (npc.isDead()) {
                handleNpcDeath(player, npc, npcDef);
                continue;
            }

            // Check if NPC should retreat
            if (npcDef.willRetreat() && npc.shouldRetreat()) {
                PlayerPacketSender.sendMessage(player, "The " + npcDef.getName() + " is retreating");
                endCombat(player, npc);
                continue;
            }

            // NPC attacks player
            int npcDamage = rollNpcDamage(npcDef, player);
            int curHP = player.getCurrentStats()[3]; // Hits stat at index 3
            int actualNpcDamage = Math.min(npcDamage, curHP);
            player.getCurrentStats()[3] = curHP - actualNpcDamage;
            player.setLastDamageTaken(actualNpcDamage);

            Logger.debug("Combat: " + npcDef.getName() + " hits " + player.getUsername() +
                         " for " + actualNpcDamage + " (HP: " + player.getCurrentStats()[3] + "/" + player.getBaseStats()[3] + ")");

            // Send player stat update (HP)
            try {
                PlayerPacketSender.sendStatUpdate(player, 3); // Hits
            } catch (Exception ex) {
                Logger.error("Failed to send stat update to " + player.getUsername());
            }

            // Check if player died
            if (player.getCurrentStats()[3] <= 0) {
                handlePlayerDeath(player, npc);
            }
        }
    }

    /**
     * Roll player's damage against an NPC using RSC combat formulas.
     * Formula from reference: combat.js
     */
    private int rollPlayerDamage(Player player, NpcDefinition npcDef) {
        // Player accuracy
        double attackLevel = player.getCurrentStats()[0]; // Attack
        double styleBonus = getStyleBonus(player.getCombatStyle(), "attack");
        double accuracy = (attackLevel + styleBonus) * (player.getBonusWeaponAim() / 600.0 + 0.1);

        // NPC protection
        double npcDefense = npcDef.getDefense() * (1.0 / 600 + 0.1);

        // Player max hit
        double strengthLevel = player.getCurrentStats()[2]; // Strength
        double strStyleBonus = getStyleBonus(player.getCombatStyle(), "strength");
        int maxHit = (int) Math.ceil((strengthLevel + strStyleBonus) * (player.getBonusWeaponPower() / 600.0 + 0.1));
        if (maxHit < 1) maxHit = 1;

        // Hit check
        int odds = (int) Math.floor(Math.min(212, (255.0 * accuracy) / (npcDefense * 4)));
        if (random.nextInt(256) > odds) {
            return 0; // Miss
        }

        // Damage roll (1 to maxHit, roughly normal distribution)
        if (maxHit <= 1) return maxHit;
        int damage = random.nextInt(maxHit) + 1;
        return damage;
    }

    /**
     * Roll NPC's damage against a player using RSC combat formulas.
     */
    private int rollNpcDamage(NpcDefinition npcDef, Player player) {
        // NPC accuracy
        double npcAttack = npcDef.getAttack() * (1.0 / 600 + 0.1);

        // Player protection
        double defenseLevel = player.getCurrentStats()[1]; // Defense
        double defStyleBonus = getStyleBonus(player.getCombatStyle(), "defense");
        double protection = (defenseLevel + defStyleBonus) * (player.getBonusArmour() / 600.0 + 0.1);

        // NPC max hit
        int maxHit = (int) Math.ceil(npcDef.getStrength() * (1.0 / 600 + 0.1));
        if (maxHit < 1) maxHit = 1;

        // Hit check
        int odds = (int) Math.floor(Math.min(212, (255.0 * npcAttack) / (protection * 4)));
        if (random.nextInt(256) > odds) {
            return 0; // Miss
        }

        // Damage roll
        if (maxHit <= 1) return maxHit;
        return random.nextInt(maxHit) + 1;
    }

    /**
     * Get style bonus for a given combat style and skill.
     * Style 0 = controlled (+1 to all), 1 = strength (+3), 2 = attack (+3), 3 = defense (+3)
     */
    private double getStyleBonus(int combatStyle, String skill) {
        if (combatStyle == 0) return 1; // Controlled
        switch (skill) {
            case "strength": return combatStyle == 1 ? 3 : 0;
            case "attack":   return combatStyle == 2 ? 3 : 0;
            case "defense":  return combatStyle == 3 ? 3 : 0;
            default: return 0;
        }
    }

    /**
     * Handle NPC death: grant XP, drop loot, start respawn timer.
     */
    private void handleNpcDeath(Player player, Npc npc, NpcDefinition def) throws Exception {
        Logger.info(player.getUsername() + " killed " + def.getName());

        // Grant combat XP
        // RSC formula: total XP = (def.hits * 4) distributed across melee skills
        int totalXP = def.getHits() * 4;
        int combatStyle = player.getCombatStyle();

        if (combatStyle == 0) {
            // Controlled: split evenly across attack, defense, strength, hits
            int each = totalXP / 4;
            addExperience(player, 0, each); // Attack
            addExperience(player, 1, each); // Defense
            addExperience(player, 2, each); // Strength
            addExperience(player, 3, each); // Hits
        } else if (combatStyle == 1) {
            // Aggressive: strength + hits
            addExperience(player, 2, totalXP * 3 / 4); // Strength
            addExperience(player, 3, totalXP / 4);       // Hits
        } else if (combatStyle == 2) {
            // Accurate: attack + hits
            addExperience(player, 0, totalXP * 3 / 4); // Attack
            addExperience(player, 3, totalXP / 4);       // Hits
        } else if (combatStyle == 3) {
            // Defensive: defense + hits
            addExperience(player, 1, totalXP * 3 / 4); // Defense
            addExperience(player, 3, totalXP / 4);       // Hits
        }

        // Send stat updates
        for (int i = 0; i < 4; i++) {
            PlayerPacketSender.sendStatUpdate(player, i);
        }

        PlayerPacketSender.sendMessage(player, "You have defeated the " + def.getName() + "!");

        // End combat
        endCombat(player, npc);

        // Start NPC respawn timer
        npc.startRespawnTimer();
    }

    /**
     * Handle player death: reset position, restore HP.
     */
    private void handlePlayerDeath(Player player, Npc npc) throws Exception {
        Logger.info(player.getUsername() + " was killed by an NPC");

        // End combat
        endCombat(player, npc);

        // Reset HP to base level
        player.getCurrentStats()[3] = player.getBaseStats()[3];
        PlayerPacketSender.sendStatUpdate(player, 3);

        // Teleport to spawn
        player.setX(122);
        player.setY(657);

        // Send death notification
        PlayerPacketSender.sendMessage(player, "@red@Oh dear! You are dead...");

        // TODO: Drop items, send SV_PLAYER_DIED packet
    }

    /**
     * End combat between a player and NPC.
     */
    private void endCombat(Player player, Npc npc) {
        if (player != null) {
            player.setInCombat(false);
            player.setAttackingNpc(null);
            player.setCombatTimer(0);
        }
        if (npc != null) {
            npc.endCombat();
        }
    }

    /**
     * Add experience to a stat, handling level ups.
     */
    private void addExperience(Player player, int statId, int amount) {
        player.getExperience()[statId] += amount;
        int newLevel = experienceToLevel(player.getExperience()[statId]);
        if (newLevel > player.getBaseStats()[statId]) {
            int levelDiff = newLevel - player.getBaseStats()[statId];
            player.getBaseStats()[statId] = newLevel;
            player.getCurrentStats()[statId] += levelDiff;
            try {
                PlayerPacketSender.sendMessage(player,
                    "@gre@You just advanced " + levelDiff + " " + getStatName(statId) + " level!");
            } catch (Exception ex) { /* ignore */ }
        }
    }

    /**
     * Convert total experience to level (RSC formula).
     */
    private int experienceToLevel(int experience) {
        int totalXP = 0;
        for (int level = 1; level < 100; level++) {
            totalXP += (int) (level + 300.0 * Math.pow(2.0, level / 7.0)) / 4;
            if (totalXP > experience) {
                return level;
            }
        }
        return 99;
    }

    private String getStatName(int statId) {
        String[] names = {"Attack", "Defense", "Strength", "Hits", "Ranged",
                          "Prayer", "Magic", "Cooking", "Woodcutting", "Fletching",
                          "Fishing", "Firemaking", "Crafting", "Smithing", "Mining",
                          "Herblaw", "Agility", "Thieving"};
        return (statId >= 0 && statId < names.length) ? names[statId] : "Unknown";
    }

    private void processPlayers() throws Exception {
        List<Player> snapshot = new ArrayList<>(context.getPlayers().getOnlinePlayers());
        VisibilityService visibility = context.getVisibilityService();

        for (Player player : snapshot) {
            if (player != null) {
                visibility.refreshVisibility(player);
            }
        }

        for (Player player : snapshot) {
            if (player == null) {
                continue;
            }

            if (processMovement(player)) {
                visibility.recordMovement(player);
                visibility.establishMutualVisibility(player);
            }
        }

        for (Player player : snapshot) {
            if (player == null) {
                continue;
            }

            try {
                CL_WalkHandler.sendRegionPlayersUpdate(player);
            } catch (Exception ex) {
                Logger.error("Region update failed for " + player.getUsername() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        // Send NPC region updates to all players
        for (Player player : snapshot) {
            if (player == null) {
                continue;
            }

            try {
                PlayerPacketSender.sendRegionNpcs(player);
            } catch (Exception ex) {
                Logger.error("NPC region update failed for " + player.getUsername() + ": " + ex.getMessage());
            }
        }
    }

    private boolean processMovement(Player player) {
        if (!player.hasWalkSteps()) {
            player.setWalking(false);
            return false;
        }

        int[] step = player.getWalkQueue().poll();
        if (step == null) {
            player.setWalking(false);
            return false;
        }

        int deltaX = step[0];
        int deltaY = step[1];
        int oldX = player.getX();
        int oldY = player.getY();

    player.setX(oldX + deltaX);
    player.setY(oldY + deltaY);
    int direction = calculateDirection(deltaX, deltaY);
    player.setDirection(direction);
    player.setWalking(true);

        Logger.debug("Player " + player.getUsername() + " walked: (" + oldX + "," + oldY + ") -> (" + player.getX() + "," + player.getY() + ") dir=" + direction);
        return true;
    }

    private int calculateDirection(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY < 0) return 0;
        if (deltaX > 0 && deltaY < 0) return 1;
        if (deltaX > 0 && deltaY == 0) return 2;
        if (deltaX > 0 && deltaY > 0) return 3;
        if (deltaX == 0 && deltaY > 0) return 4;
        if (deltaX < 0 && deltaY > 0) return 5;
        if (deltaX < 0 && deltaY == 0) return 6;
        if (deltaX < 0 && deltaY < 0) return 7;
        return 0;
    }

    public long getTickCount() {
        return tickCount;
    }
}
