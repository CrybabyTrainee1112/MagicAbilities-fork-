package net.trduc.magicabilities.powers.executions;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveExecute extends Execute{
    public MoveExecute(PlayerMoveEvent event, Player player) {
        super(event, player);
    }
}
