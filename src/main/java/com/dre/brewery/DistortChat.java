/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024 The Brewery Team
 *
 * This file is part of BreweryX.
 *
 * BreweryX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreweryX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreweryX. If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package com.dre.brewery;

import com.dre.brewery.api.events.PlayerChatDistortEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.configuration.sector.capsule.ConfigDistortWord;
import com.dre.brewery.utility.Logging;
import lombok.Getter;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistortChat {

    // represents Words and letters, that are replaced in drunk players messages
    private static final Config config = ConfigManager.getConfig(Config.class);
    private static final Lang lang = ConfigManager.getConfig(Lang.class);

    public static final List<DistortChat> words = new ArrayList<>();
    @Getter
    public static final List<String> commands = new ArrayList<>();
    private static final List<String> playerParameterCommands = Arrays.asList("/msg", "/tell", "/whisper", "/w"); // e.g. '/msg PLAYER ...' -> don't distort the player name here
    @Getter
    public static final List<String[]> ignoreText = new ArrayList<>();
    private static final Map<String, Long> waitPlayers = new HashMap<>();

    private String from;
    private String to;
    private String[] pre;
    private Boolean match = false;
    private int alcohol = 1;
    private int percentage = 100;

    public DistortChat(Map<?, ?> part) {
        for (Map.Entry<?, ?> wordPart : part.entrySet()) {
            String key = (String) wordPart.getKey();

            if (wordPart.getValue() instanceof String) {

                if (key.equalsIgnoreCase("replace")) {
                    this.from = (String) wordPart.getValue();
                } else if (key.equalsIgnoreCase("to")) {
                    this.to = (String) wordPart.getValue();
                } else if (key.equalsIgnoreCase("pre")) {
                    String fullPre = (String) wordPart.getValue();
                    this.pre = fullPre.split(",");
                }

            } else if (wordPart.getValue() instanceof Boolean) {

                if (key.equalsIgnoreCase("match")) {
                    this.match = (Boolean) wordPart.getValue();
                }

            } else if (wordPart.getValue() instanceof Integer) {

                if (key.equalsIgnoreCase("alcohol")) {
                    this.alcohol = (Integer) wordPart.getValue();
                } else if (key.equalsIgnoreCase("percentage")) {
                    this.percentage = (Integer) wordPart.getValue();
                }

            }
        }
        if (this.from != null && this.to != null) {
            words.add(this);
        }
    }

    public DistortChat(ConfigDistortWord configDistortWord) {
        this.from = configDistortWord.getReplace();
        this.to = configDistortWord.getTo();

        String pre = configDistortWord.getPre();
        if (pre != null && !pre.isEmpty()) {
            this.pre = pre.split(",");
        } else {
            this.pre = null;
        }
        this.match = configDistortWord.getMatch() != null ? configDistortWord.getMatch() : false;
        this.alcohol = configDistortWord.getAlcohol() != null ? configDistortWord.getAlcohol() : 1;
        this.percentage = configDistortWord.getPercentage() != null ? configDistortWord.getPercentage() : 100;

        if (this.from != null && this.to != null) {
            words.add(this);
        }
    }

    // Distort players words when he uses a command
    public static void playerCommand(PlayerCommandPreprocessEvent event) {
        BPlayer bPlayer = BPlayer.get(event.getPlayer());
        if (bPlayer == null) {
            return;
        }
        if (!commands.isEmpty() && !words.isEmpty()) {
            String name = event.getPlayer().getName();
            if (!waitPlayers.containsKey(name) || waitPlayers.get(name) + 500 < System.currentTimeMillis()) {
                String chat = event.getMessage();
                for (String command : commands) {
                    if (command.length() + 1 < chat.length()) {
                        if (Character.isSpaceChar(chat.charAt(command.length()))) {
                            if (chat.toLowerCase().startsWith(command.toLowerCase())) {
                                if (config.isLogRealChat()) {
                                    Logging.log(lang.getEntry("Player_TriedToSay", name, chat));
                                }

                                // exclude player parameters
                                String message = playerParameterCommands.contains(command.toLowerCase())
                                    ? chat.substring(chat.indexOf(' ', chat.indexOf(' ', 0) + 1) + 1).trim()
                                    : chat.substring(chat.indexOf(' ') + 1).trim();

                                String distorted = distortMessage(message, bPlayer.getDrunkeness());
                                PlayerChatDistortEvent call = new PlayerChatDistortEvent(event.isAsynchronous(), event.getPlayer(), bPlayer, message, distorted);
                                BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(call);
                                if (call.isCancelled()) {
                                    return;
                                }
                                distorted = call.getDistortedMessage();

                                // reassemble command
                                event.setMessage(playerParameterCommands.contains(command.toLowerCase())
                                    ? chat.substring(0, chat.indexOf(' ', chat.indexOf(' ', 0) + 1) + 1) + distorted
                                    : chat.substring(0, chat.indexOf(' ') + 1) + distorted);

                                waitPlayers.put(name, System.currentTimeMillis());
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    // Distort players words when he uses a command
    public static void signWrite(SignChangeEvent event) {
        BPlayer bPlayer = BPlayer.get(event.getPlayer());
        if (bPlayer != null) {
            if (!words.isEmpty()) {
                int index = 0;
                for (String message : event.getLines()) {
                    if (message.length() > 1) {
                        String distorted = distortMessage(message, bPlayer.getDrunkeness());
                        PlayerChatDistortEvent call = new PlayerChatDistortEvent(event.isAsynchronous(), event.getPlayer(), bPlayer, message, distorted);
                        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(call);
                        if (!call.isCancelled()) {
                            distorted = call.getDistortedMessage();

                            if (distorted.length() > 15) {
                                distorted = distorted.substring(0, 14);
                            }
                            event.setLine(index, distorted);
                        }
                    }
                    index++;
                }
            }
        }
    }

    // Distort players words when he talks
    public static void playerChat(AsyncPlayerChatEvent event) {
        BPlayer bPlayer = BPlayer.get(event.getPlayer());
        if (bPlayer != null) {
            if (!words.isEmpty()) {
                String message = event.getMessage();
                if (config.isLogRealChat()) {
                    Logging.log(lang.getEntry("Player_TriedToSay", event.getPlayer().getName(), message));
                }

                String distorted = distortMessage(message, bPlayer.getDrunkeness());
                PlayerChatDistortEvent call = new PlayerChatDistortEvent(event.isAsynchronous(), event.getPlayer(), bPlayer, message, distorted);
                BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(call);
                if (call.isCancelled()) {
                    return;
                }
                distorted = call.getDistortedMessage();

                event.setMessage(distorted);
            }
        }
    }

    // distorts a message, ignoring text enclosed in ignoreText letters
    public static String distortMessage(String message, int drunkenness) {
        if (!ignoreText.isEmpty()) {
            for (String[] bypass : ignoreText) {
                int indexStart = 0;
                if (!bypass[0].equals("")) {
                    indexStart = message.indexOf(bypass[0]);
                }
                int indexEnd = message.length() - 1;
                if (!bypass[1].equals("")) {
                    indexEnd = message.indexOf(bypass[1], indexStart + 2);
                }
                if (indexStart != -1 && indexEnd != -1) {
                    if (indexEnd > indexStart + 1) {
                        String ignoredMessage = message.substring(indexStart, indexEnd);
                        String msg0 = message.substring(0, indexStart);
                        String msg1 = message.substring(indexEnd);

                        if (msg0.length() > 1) {
                            msg0 = distortMessage(msg0, drunkenness);
                        }
                        if (msg1.length() > 1) {
                            msg1 = distortMessage(msg1, drunkenness);
                        }

                        return msg0 + ignoredMessage + msg1;
                    }
                }
            }
        }
        return distortString(message, drunkenness);
    }

    // distorts a message without checking ignoreText letters
    private static String distortString(String message, int drunkenness) {

        if (message.length() > 1) {
            // Create our own reference to the words list, in case of config reload
            List<DistortChat> words = DistortChat.words;
            for (DistortChat word : words) {

                if (word.alcohol <= drunkenness) {
                    message = word.distort(message);
                }
            }
        }
        return message;
    }

    // replace "percent"% of "from" -> "to" in "words", when the string before
    // each "from" "match"es "pre"
    // Not yet ignoring case :(
    public String distort(String words) {
        String from = this.from;
        String to = this.to;

        if (from.equalsIgnoreCase("-end")) {
            from = words;
            to = words + to;
        } else if (from.equalsIgnoreCase("-start")) {
            from = words;
            to = to + words;
        } else if (from.equalsIgnoreCase("-all")) {
            from = words;
        } else if (from.equalsIgnoreCase("-space")) {
            from = " ";
        } else if (from.equalsIgnoreCase("-random")) {
            // inserts "to" on a random position in "words"
            int charIndex = (int) (Math.random() * (words.length() - 1));
            if (charIndex < words.length() / 2) {
                from = words.substring(charIndex);
                to = to + from;
            } else {
                from = words.substring(0, charIndex);
                to = from + to;
            }
        }

        if (words.contains(from)) {
            // some characters (*,?) disturb split() which then throws
            // PatternSyntaxException
            try {
                if (pre == null && percentage == 100) {
                    // All occurences of "from" need to be replaced
                    return words.replaceAll(from, to);
                }
                StringBuilder newWords = new StringBuilder();
                if (words.endsWith(from)) {
                    // add space to end to recognize last occurence of "from"
                    words = words + " ";
                }
                // remove all "from" and split "words" there
                String[] splitted = words.split(java.util.regex.Pattern.quote(from));
                int index = 0;
                String part;

                // if there are occurences of "from"
                if (splitted.length > 1) {
                    // - 1 because dont add "to" to the end of last part
                    while (index < splitted.length - 1) {
                        part = splitted[index];
                        // add current part of "words" to the output
                        newWords.append(part);
                        // check if the part ends with correct string

                        if (doesPreMatch(part) && Math.random() * 100.0 <= percentage) {
                            // add replacement
                            newWords.append(to);
                        } else {
                            // add original
                            newWords.append(from);
                        }
                        index++;
                    }
                    // add the last part to finish the sentence
                    part = splitted[index];
                    if (part.equals(" ")) {
                        // dont add the space to the end
                        return newWords.toString();
                    } else {
                        return newWords.append(part).toString();
                    }
                }
            } catch (java.util.regex.PatternSyntaxException e) {
                // e.printStackTrace();
                return words;
            }
        }
        return words;
    }

    public boolean doesPreMatch(String part) {
        boolean isBefore = !match;
        if (pre != null) {
            for (String pr : pre) {
                if (part.endsWith(pr)) {
                    // If a match is wanted set isBefore to true, else to false
                    isBefore = match;
                    break;
                }
            }
        } else {
            isBefore = true;
        }
        return isBefore;
    }

}
