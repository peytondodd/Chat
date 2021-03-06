package com.winthier.chat.event;

import com.winthier.chat.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter @RequiredArgsConstructor
public final class ChatPlayerTalkEvent extends Event implements Cancellable {
    // Event Stuff

    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Setter private boolean cancelled = false;

    // Chat Stuff

    private final Player player;
    private final Channel channel;
    private final String message;

    public static boolean call(Player player, Channel channel, String msg) {
        ChatPlayerTalkEvent event = new ChatPlayerTalkEvent(player, channel, msg);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return (!event.isCancelled());
    }
}
