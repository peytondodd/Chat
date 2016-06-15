package com.winthier.chat;

import com.winthier.chat.MessageFilter;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.Option;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLPattern;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length == 0) {
            showMenu(player);
            return true;
        }
        String firstArg = args[0].toLowerCase();
        if (firstArg.equals("set")) {
            if (player == null) return false;
            UUID uuid = player.getUniqueId();
            if (args.length == 1) {
            } else if (args.length == 2) {
                Channel channel = ChatPlugin.getInstance().findChannel(args[1]);
                if (channel == null || !channel.hasPermission(player)) return false;
                showSettingsMenu(player, channel);
            } else if (args.length == 3 && args[2].equals("reset")) {
                Channel channel = ChatPlugin.getInstance().findChannel(args[1]);
                if (channel == null || !channel.hasPermission(player)) return false;
                for (Option option: channel.getOptions()) {
                    SQLSetting.set(uuid, channel.getKey(), option.key, null);
                }
                Msg.info(player, "&aSettings reset to default");
                showSettingsMenu(player, channel);
            } else if (args.length == 4) {
                Channel channel = ChatPlugin.getInstance().findChannel(args[1]);
                if (channel == null || !channel.hasPermission(player)) return false;
                String key = args[2];
                String value = args[3];
                if (!isKeyValuePairValid(channel.getOptions(), key, value)) {
                    return false;
                }
                SQLSetting.set(uuid, channel.getKey(), key, value);
                Msg.info(player, "&aSettings updated");
                showSettingsMenu(player, channel);
            } else {
                return false;
            }
        } else if (firstArg.equals("list") && args.length == 1) {
            listChannels(player);
        } else if (firstArg.equals("join") && args.length == 2) {
            Channel channel = ChatPlugin.getInstance().findChannel(args[1]);
            if (channel == null || !channel.hasPermission(player)) return false;
            channel.joinChannel(player.getUniqueId());
            listChannels(player);
        } else if (firstArg.equals("leave") && args.length == 2) {
            Channel channel = ChatPlugin.getInstance().findChannel(args[1]);
            if (channel == null || !channel.hasPermission(player)) return false;
            channel.leaveChannel(player.getUniqueId());
            listChannels(player);
        }
        return true;
    }

    private boolean isKeyValuePairValid(List<Option> options, String key, String value) {
        for (Option option: options) {
            if (key.equals(option.key)) {
                for (Option.State state: option.states) {
                    if (value.equals(state.value)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    void showMenu(Player player) {
        if (player == null) return;
        Msg.info(player, "&3Menu");
        List<Object> json = new ArrayList<>();
        json.add(Msg.format("&oChannel Settings"));
        for (Channel channel: ChatPlugin.getInstance().getChannels()) {
            if (!channel.hasPermission(player)) continue;
            json.add(" ");
            json.add(Msg.button("&r["+SQLSetting.getChatColor(player.getUniqueId(), channel.getKey(), "ChannelColor", ChatColor.WHITE)+channel.getTag()+"&r]", channel.getTitle(), "/ch set "+channel.getKey()));
        }
        Msg.raw(player, json);
        json.clear();
        json.add(Msg.format("&oChannel List "));
        json.add(Msg.button(ChatColor.BLUE, "&r[&9List&r]", "Channel List", "/ch list"));
        Msg.raw(player, json);
    }

    void showSettingsMenu(Player player, Channel channel) {
        UUID uuid = player.getUniqueId();
        Msg.info(player, SQLSetting.getChatColor(uuid, channel.getKey(), "ChannelColor", ChatColor.WHITE) + channel.getTitle() + " Settings");
        List<Object> json = new ArrayList<>();
        for (Option option: channel.getOptions()) {
            json.clear();
            json.add(Msg.format("&o %s", option.displayName));
            for (Option.State state: option.states) {
                json.add(" ");
                String current = SQLSetting.getString(uuid, channel.getKey(), option.key, option.defaultValue);
                boolean active = false;
                if (current != null && current.equals(state.value)) active = true;
                if (active) {
                    json.add(Msg.button("&r["+state.activeColor+state.displayName+"&r]", state.description, "/ch set "+channel.getKey()+" "+option.key+" "+state.value));
                } else {
                    json.add(Msg.button(state.color+state.displayName, state.description, "/ch set "+channel.getKey()+" "+option.key+" "+state.value));
                }
            }
            Msg.raw(player, json);
        }
        json.clear();
        json.add(Msg.button(ChatColor.DARK_RED, "&r[&4Reset&r]", "&4Reset to channel defaults.", "/ch set "+channel.getKey()+" reset"));
        Msg.raw(player, json);
    }

    void listChannels(Player player) {
        if (player == null) return;
        Msg.info(player, "3Channel List");
        for (Channel channel: ChatPlugin.getInstance().getChannels()) {
            List<Object> json = new ArrayList<>();
            if (!channel.hasPermission(player)) continue;
            json.add(" ");
            if (channel.isJoined(player.getUniqueId())) {
                json.add(Msg.button(ChatColor.GREEN, "x", "Leave " + channel.getTitle(), "/ch leave " + channel.getKey()));
            } else {
                json.add(Msg.button(ChatColor.RED, "o", "Join " + channel.getTitle(), "/ch join " + channel.getKey()));
            }
            json.add(" ");
            json.add(Msg.button(SQLSetting.getChatColor(player.getUniqueId(), channel.getKey(), "ChannelColor", ChatColor.WHITE), channel.getTitle(), null, null));
            json.add(Msg.button(ChatColor.DARK_GRAY, " - ", null, null));
            json.add(Msg.button(ChatColor.GRAY, channel.getDescription(), null, null));
            Msg.raw(player, json);
        }
    }
}
