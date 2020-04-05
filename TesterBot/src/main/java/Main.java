
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.ChannelManager;
//import net.dv8tion.jda.core.*;

import javax.security.auth.login.LoginException;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Main extends ListenerAdapter {
    private static JDA builder;
    private static String botid = "696098281247473744";
    private boolean ready = false;
    private MessageChannel channel;
    private List<String> players = new ArrayList<>();;
    private String tournament_message = "0";
    private int numofplayers = 8;
    private boolean started = false;
    private static Instant start;
    private Duration timeElapsed;
    private String joinMessage = "If anyone wants to join react to this!";

    public static void main(String[] args) throws Exception {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("../TourneyToken")), "UTF8"));
            builder = new JDABuilder(AccountType.BOT).setToken(reader.readLine()).build();
            reader.close();
            builder.addEventListener(new Main());
            start = Instant.now();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        HashMap<String, Integer> roles = new HashMap<>();
        String author = event.getAuthor().getId();

        List<Role> role =  event.getGuild().getMemberById(author).getRoles();
        for (Role r : role) {
            roles.put(r.getName(),0);
        }

        Instant end = Instant.now();
        timeElapsed = Duration.between(start, end);
        if (timeElapsed.toMinutes() >= 5) {
            players = new ArrayList<>();
            started = false;
        }

        Message message = event.getMessage();


        EmbedBuilder embedBuilder = new EmbedBuilder();
        channel = event.getChannel();

        if (event.getAuthor().getId().equals(botid) && message.getContentRaw().equals(joinMessage)) {

            if (tournament_message.length() > 2) {
                channel.deleteMessageById(tournament_message).queue();
            }

            tournament_message = message.getId();
        }

        if (event.getChannel().getName().equals("general") && !event.getAuthor().getId().equals(botid) && roles.containsKey("Player") ) {
            Guild guild = event.getGuild();
            String content = message.getContentRaw();
            String[] split = content.split("\\s+", -1);
            List<Member> mentioned = message.getMentionedMembers();



            if (isTarget(split[0], botid) && split[1].toLowerCase().equals("help")) {

                embedBuilder.setTitle("How this bot works", null);
                embedBuilder.setDescription("This is a bot which helps create teams for a 2's tourneys. The lobby will be reset after 5 minutes of creating the teams. This bot can only be used by people with the player role");
                embedBuilder.addField("Commands", "new, add, addfromvc, remove, remix, restart. All commands (except 'addfromvc') work by first @ing tourneyBot typing the command and then passing arguments", false);
                embedBuilder.addField("new", "tourneyBot writes a message and anyone who reacts to it will be added to the tourney lobby", true);
                embedBuilder.addField("add", "Anyone can add anyone else into the lobby by calling this command and @ing whoever they want to join", true);
                embedBuilder.addField("addfromvc", "All players in the same voice channel as the player who calls this is added to the tournament", true);
                embedBuilder.addField("remove", "Anyone can remove anyone else into the lobby by calling this command and @ing whoever they want to leave", true);
                embedBuilder.addField("remix", "remixes the teams", true);
                embedBuilder.addField("restart", "recreates the lobby", true);
                embedBuilder.addField("end", "destroys the lobby", true);
                embedBuilder.addField("help", "shows the help message", true);
                embedBuilder.addField("debug", "shows who's currently in the lobby", true);
                MessageEmbed m = embedBuilder.build();

                channel.sendMessage(m).queue();
                return;
            }

            if (isTarget(split[0], botid) && split[1].toLowerCase().equals("debug")) {
                String s = "```" + "\n";
                for (String p : players) {
                    s += p + "\n";
                    System.out.println(p);
                }
                s += "```";
                channel.sendMessage(s).queue();
                return;
            }
            if (isTarget(split[0], botid) && split[1].toLowerCase().equals("end")) {
                channel.sendMessage("Lobby destroyed, thanks for that <@!" + author + ">" ).queue();
                players = new ArrayList<>();
                started = false;
            }
            if (isTarget(split[0], botid) && split[1].toLowerCase().equals("remove")){
                for (Member m : mentioned) {
                    if (players.contains(getName(m))) {
                        System.out.println("Player: " + getName(m) + " has been removed. Number of players is  : " + (players.size()+1));
                        players.remove(getName(m));
                        channel.sendMessage(getName(m) + " has been removed from queue").queue();
                    }
                }
                if (players.size() < numofplayers) {
                    started = false;
                }

            }

            if (!started) {

                if (isTarget(split[0], botid) && split[1].toLowerCase().equals("addfromvc")) {
                    try {
                        VoiceChannel vc = Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel();
                        if (vc != null) {
                            List<Member> membersInVc = vc.getManager().getChannel().getMembers();

                            addPlayer(membersInVc);
                            channel.sendMessage("```Added all players in " + vc.getName() + "```").queue();

                        }

                    } catch (NullPointerException ne) {
                        ne.printStackTrace();
                    }
                }

                if (isTarget(split[0], botid) && split[1].toLowerCase().equals("new")) {
                    channel.sendMessage("```Starting new tournament```").queue();
                    String m = joinMessage;

                    channel.sendMessage(m).queue(message1 -> {message1.addReaction(EmojiParser.parseToUnicode(":thumbsup:")).queue();});
                }
                if (isTarget(split[0], botid) && split[1].toLowerCase().equals("add")){
                   addPlayer(mentioned);


                }
            } else {
                if (isTarget(split[0], botid) && split[1].toLowerCase().equals("remix")) {
                    createTournament();
                    return;
                }
                if (isTarget(split[0], botid) && split[1].toLowerCase().equals("restart")) {
                    players = new ArrayList<>();
                    started = false;
                    String m = joinMessage;

                    channel.sendMessage(m).queue();
                    return;
                }

            }




            // if next word is !r (for random) or !c (for captains)
            // enter all players names

            // (random team generator)
            // create random 2s teams.
            // create a bracket
            // give :thumbsup: and :thumbsdown: to vote
            // if more than half thumbs down then recreate the teams

            // directly implement results into ladder



        }
    }

    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.getUserId().equals(botid) && event.getMessageId().equals(tournament_message)) {
            Guild guild = event.getGuild();
            Member member = guild.getMember((event.getUser()));
            if (!players.contains(getName(member)) && players.size() < numofplayers) {
                players.add(getName(member));
                if (players.size() == numofplayers) {
                    createTournament();
                }
            }

        }


    }
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (event.getMessageId().equals(tournament_message)) {
            Guild guild = event.getGuild();
            Member member = guild.getMember((event.getUser()));
            players.remove(getName(member));
        }

    }

    private String getName(Member member)
    {
        return member.getEffectiveName().replaceAll(" \".*\" ", " ");
    }

    private boolean isTarget(String arg, String id) {
//        for (String arg : args) {
        if (arg.equals("<@!"+id+">")||arg.equals("<@"+id+">")) {
            return true;
        }
//        }
        return false;
    }

    private void addPlayer(List<Member> members) {
        for (Member m : members) {
            if (!m.getId().equals(botid) && !players.contains(getName(m))) {
                System.out.println("Player: " + getName(m) + " has been added. Number of players is  : " + (players.size()+1));
                if (players.size() < numofplayers) {
                    players.add(getName(m));
                    if (players.size() == numofplayers) {
                        createTournament();
                    }
                }
            }

        }
        if (players.size() < numofplayers) {

            String m = joinMessage;
            channel.sendMessage(m).queue(message1 -> {message1.addReaction(EmojiParser.parseToUnicode(":thumbsup:")).queue();});
        }
    }
    private void createTournament() {
        //get players
        start = Instant.now();
        started = true;
        List<String> currentplayers = new ArrayList<>();
        for (String player : players) {
            currentplayers.add(player);
        }
        System.out.println("Start tournament also players .size() = " + players.size());
        Set<String> team1 = new HashSet<>();
        Set<String> team2 = new HashSet<>();
        Set<String> team3 = new HashSet<>();
        Set<String> team4 = new HashSet<>();
        Random rand = new Random();
        String[] ordered = new String[currentplayers.size()];



        int i = 0;
        while (currentplayers.size() > 0) {
            int r = rand.nextInt(currentplayers.size());
            System.out.println("R is: " + r + " Player is: " + currentplayers.get(r) + " i is: " + i);
            ordered[i] = currentplayers.get(r);
            currentplayers.remove(r);
            i++;
        }
        for (int n = 0; n < ordered.length; n++){
            System.out.println(ordered[n]);
        }
        team1.add(ordered[0]);
        team1.add(ordered[1]);
        team2.add(ordered[2]);
        team2.add(ordered[3]);
        team3.add(ordered[4]);
        team3.add(ordered[5]);
        team4.add(ordered[6]);
        team4.add(ordered[7]);


        // print teams and brackets

        String message = "```\nTeam 1: " + ordered[0] + " and " + ordered[1] + "\n" +
                "Team 2: " + ordered[2] + " and " + ordered[3] + "\n" +
                "Team 3: " + ordered[4] + " and " + ordered[5] + "\n" +
                "Team 4: " + ordered[6] + " and " + ordered[7] + " ```";
        channel.sendMessage(message).queue();
        System.out.println("players.size() is now = " + players.size());
    }
}
