/**
 * Represents a static NPC type definition loaded from config/npcs.json.
 * Each NPC type has fixed stats, name, and behavior properties.
 */
public class NpcDefinition {
    private final int id;
    private final String name;
    private final String description;
    private final String command;
    private final int attack;
    private final int strength;
    private final int hits;
    private final int defense;
    private final String hostility; // null, "retreats", "combative", "aggressive"
    private final int combatAnimation;

    public NpcDefinition(int id, String name, String description, String command,
                         int attack, int strength, int hits, int defense,
                         String hostility, int combatAnimation) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.command = command;
        this.attack = attack;
        this.strength = strength;
        this.hits = hits;
        this.defense = defense;
        this.hostility = hostility;
        this.combatAnimation = combatAnimation;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCommand() { return command; }
    public int getAttack() { return attack; }
    public int getStrength() { return strength; }
    public int getHits() { return hits; }
    public int getDefense() { return defense; }
    public String getHostility() { return hostility; }
    public int getCombatAnimation() { return combatAnimation; }

    /**
     * Calculate combat level using RSC formula: floor((atk + def + str + hp) / 4)
     */
    public int getCombatLevel() {
        return (attack + defense + strength + hits) / 4;
    }

    public boolean isAttackable() {
        return hits > 0;
    }

    public boolean isAggressive() {
        return "aggressive".equals(hostility);
    }

    public boolean isCombative() {
        return "combative".equals(hostility);
    }

    public boolean willRetreat() {
        return "retreats".equals(hostility);
    }

    @Override
    public String toString() {
        return "NpcDef{id=" + id + ", name='" + name + "', combat=" + getCombatLevel() + "}";
    }
}
