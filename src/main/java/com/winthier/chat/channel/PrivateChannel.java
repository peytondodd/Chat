package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@Getter
public class PrivateChannel extends AbstractChannel {
    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission("chat.pm");
    }

    public void handleMessage(Message message) {
        Player player = Bukkit.getServer().getPlayer(message.sender);
        if (player != null) sendMessage(message, player, message.special != null);
    }

    void sendMessage(Message message, Player player, boolean ack) {
        UUID uuid = player.getUniqueId();
        String key = getKey();
        fillMessage(message);
        List<Object> json = new ArrayList<>();
        ChatColor channelColor = SQLSetting.getChatColor(uuid, key, "ChannelColor", ChatColor.WHITE);
        ChatColor textColor = SQLSetting.getChatColor(uuid, key, "TextColor", ChatColor.WHITE);
        ChatColor senderColor = SQLSetting.getChatColor(uuid, key, "SenderColor", ChatColor.WHITE);
        ChatColor bracketColor = SQLSetting.getChatColor(uuid, key, "BracketColor", ChatColor.WHITE);
        boolean tagPlayerName = SQLSetting.getBoolean(uuid, key, "TagPlayerName", false);
        BracketType bracketType = BracketType.of(SQLSetting.getString(uuid, key, "BracketType", "angle"));
        json.add("");
        // Channel Tag
        if (SQLSetting.getBoolean(uuid, key, "ShowChannelTag", true)) {
            json.add(channelTag(channelColor, bracketColor, bracketType));
        }
        // Server Name
        if (message.senderServer != null && SQLSetting.getBoolean(uuid, key, "ShowServer", true)) {
            json.add(serverTag(message, channelColor, bracketColor, bracketType));
        }
        // From/To
        json.add(Msg.button(senderColor, ack?"to":"from", null, null));
        json.add(" ");
        // Player Title
        if (SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", true)) {
            json.add(senderTitleTag(message, bracketColor, bracketType));
        }
        // Player Name
        json.add(senderTag(message, senderColor, bracketColor, bracketType, tagPlayerName));
        json.add(Msg.button(bracketColor, ":", null, null));
        json.add(" ");
        // Message
        appendMessage(json, message, textColor, SQLSetting.getBoolean(uuid, key, "LanguageFilter", true));
        Msg.raw(player, json);
        // Reply
        SQLSetting.set(uuid, key, "ReplyName", message.senderName);
        // Ack
        if (!ack) {
            message.special = "Ack";
            message.sender = player.getUniqueId();
            message.senderName = player.getName();
            message.senderTitle = null;
            message.senderTitleDescription = null;
            message.senderServer = ChatPlugin.getInstance().getServerName();
            message.senderServerDisplayName = ChatPlugin.getInstance().getServerDisplayName();
            fillMessage(message);
            ChatPlugin.getInstance().didCreateMessage(message);
            handleMessage(message);
        }
    }

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
        UUID uuid = context.player.getUniqueId();
        final String[] arr = context.message.split("\\s+", 2);
        if (arr.length == 0) {
            return;
        }
        String targetName = arr[0];
        Chatter target = ChatPlugin.getInstance().findPlayer(targetName);
        if (target == null) {
            Msg.send(context.player, "&cPlayer not found: %s", targetName);
            return;
        }
        if (arr.length == 1) {
            setFocusChannel(uuid);
            SQLSetting.set(uuid, getKey(), "FocusName", target.getName());
            Msg.info(context.player, "Now focusing %s", target.getName());
        } else if (arr.length == 2) {
            talk(context.player, target, arr[1]);
        }
    }

    @Override
    public void playerDidUseChat(PlayerCommandContext context) {
        String focusName = SQLSetting.getString(context.player.getUniqueId(), getKey(), "FocusName", null);
        if (focusName == null) return;
        Chatter target = ChatPlugin.getInstance().findPlayer(focusName);
        if (target == null) {
            Msg.send(context.player, "&cPlayer not found: %s", focusName);
            return;
        }
        talk(context.player, target, context.message);
    }

    void reply(PlayerCommandContext context) {
        String replyName = SQLSetting.getString(context.player.getUniqueId(), getKey(), "ReplyName", null);
        if (replyName == null) return;
        Chatter target = ChatPlugin.getInstance().findPlayer(replyName);
        if (target == null) {
            Msg.send(context.player, "&cPlayer not found: %s", replyName);
            return;
        }
        if (context.message == null || context.message.isEmpty()) {
            UUID uuid = context.player.getUniqueId();
            setFocusChannel(uuid);
            SQLSetting.set(uuid, getKey(), "FocusName", target.getName());
            Msg.info(context.player, "Now focusing %s", target.getName());
        } else {
            talk(context.player, target, context.message);
        }
    }

    void talk(Player player, Chatter target, String msg) {
        SQLLog.store(player, this, target.getUuid().toString(), msg);
        Message message = makeMessage(player, msg);
        message.target = target.getUuid();
        message.targetName = target.getName();
        ChatPlugin.getInstance().didCreateMessage(message);
        handleMessage(message);
    }
}
