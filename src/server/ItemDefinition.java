import java.util.List;

/**
 * Represents a static item definition loaded from config/items.json + wieldable.json.
 * Each item in the game has a fixed definition describing its properties.
 */
public class ItemDefinition {
    private final int id;
    private final String name;
    private final String description;
    private final String command;
    private final int sprite;
    private final int price;
    private final boolean stackable;
    private final boolean special;
    private final boolean members;
    private final boolean untradeable;
    private final List<String> equipSlots; // null if not equippable

    // Wieldable bonuses (from wieldable.json), 0 if not wieldable
    private int animation;
    private int armour;
    private int weaponAim;
    private int weaponPower;
    private int magic;
    private int prayer;

    public ItemDefinition(int id, String name, String description, String command,
                          int sprite, int price, boolean stackable, boolean special,
                          boolean members, boolean untradeable, List<String> equipSlots) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.command = command;
        this.sprite = sprite;
        this.price = price;
        this.stackable = stackable;
        this.special = special;
        this.members = members;
        this.untradeable = untradeable;
        this.equipSlots = equipSlots;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCommand() { return command; }
    public int getSprite() { return sprite; }
    public int getPrice() { return price; }
    public boolean isStackable() { return stackable; }
    public boolean isSpecial() { return special; }
    public boolean isMembers() { return members; }
    public boolean isUntradeable() { return untradeable; }
    public List<String> getEquipSlots() { return equipSlots; }

    public boolean isEquippable() { return equipSlots != null && !equipSlots.isEmpty(); }
    public boolean isWieldable() { return animation > 0 || armour > 0 || weaponAim > 0 || weaponPower > 0; }

    public int getAnimation() { return animation; }
    public int getArmour() { return armour; }
    public int getWeaponAim() { return weaponAim; }
    public int getWeaponPower() { return weaponPower; }
    public int getMagic() { return magic; }
    public int getPrayer() { return prayer; }

    public void setWieldableStats(int animation, int armour, int weaponAim, int weaponPower, int magic, int prayer) {
        this.animation = animation;
        this.armour = armour;
        this.weaponAim = weaponAim;
        this.weaponPower = weaponPower;
        this.magic = magic;
        this.prayer = prayer;
    }

    @Override
    public String toString() {
        return "ItemDef{id=" + id + ", name='" + name + "', stackable=" + stackable + "}";
    }
}
