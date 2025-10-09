// https://classic.runescape.wiki/w/Shopkeeper_(Lumbridge)

const { canIHelpYou } = require('../general-shopkeeper');

const SHOPKEEPER_IDS = new Set([55, 83]);

async function onTalkToNPC(player, npc) {
    if (!SHOPKEEPER_IDS.has(npc.id)) {
        return false;
    }

    return await canIHelpYou(player, npc, 'lumbridge-general');
}

module.exports = { onTalkToNPC };
