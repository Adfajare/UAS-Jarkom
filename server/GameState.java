import java.util.*;

public class GameState {
    private Map<String, Player> players;
    private Set<Integer> bombs;
    private Map<String, Set<Integer>> playerGuesses;
    private Set<String> stoppedPlayers;

    private static final int MAX_NUMBER = 70; // Maximum number range (1-70)
    private static final int BOMB_COUNT = 15; // Number of bombs to generate

    public GameState() {
        players = new HashMap<>();
        bombs = new HashSet<>();
        playerGuesses = new HashMap<>();
        stoppedPlayers = new HashSet<>();
        generateBombs();
    }

    public void reset() {
        for (Player player : players.values()) {
            player.setScore(0);
            player.setStopped(false);
        }
        playerGuesses.clear();
        stoppedPlayers.clear();
        generateBombs();
    }

    private void generateBombs() {
        generateBombs(MAX_NUMBER, BOMB_COUNT);
    }

    private void generateBombs(int maxNumber, int bombCount) {
        bombs.clear();
        Random random = new Random();

        // Ensure bomb count doesn't exceed the number range
        int actualBombCount = Math.min(bombCount, maxNumber);

        while (bombs.size() < actualBombCount) {
            bombs.add(1 + random.nextInt(maxNumber)); // Generate between 1 and maxNumber
        }
        System.out.println("Bombs placed at: " + bombs + " (Range: 1-" + maxNumber + ")");
    }

    // Getter methods for game parameters
    public static int getMaxNumber() {
        return MAX_NUMBER;
    }

    public static int getBombCount() {
        return BOMB_COUNT;
    }

    public void addPlayer(String name) {
        if (!players.containsKey(name)) {
            players.put(name, new Player(name));
            playerGuesses.put(name, new HashSet<>());
        }
    }

    public boolean isBomb(int number) {
        return bombs.contains(number);
    }

    public boolean isAlreadyGuessed(String playerName, int number) {
        Set<Integer> guesses = playerGuesses.get(playerName);
        if (guesses == null) {
            // Initialize if not exists
            guesses = new HashSet<>();
            playerGuesses.put(playerName, guesses);
            return false;
        }
        return guesses.contains(number);
    }

    public void addGuess(String playerName, int number) {
        Set<Integer> guesses = playerGuesses.get(playerName);
        if (guesses == null) {
            guesses = new HashSet<>();
            playerGuesses.put(playerName, guesses);
        }
        guesses.add(number);
    }

    public void increaseScore(String playerName, int points) {
        Player player = players.get(playerName);
        if (player != null) {
            player.setScore(player.getScore() + points);
        }
    }

    public void decreaseScore(String playerName, int points) {
        Player player = players.get(playerName);
        if (player != null) {
            player.setScore(Math.max(0, player.getScore() - points));
        }
    }

    public void setPlayerStopped(String playerName) {
        Player player = players.get(playerName);
        if (player != null) {
            player.setStopped(true);
            stoppedPlayers.add(playerName);
        }
    }

    public boolean allPlayersStopped() {
        return stoppedPlayers.size() == players.size() && players.size() > 0;
    }

    public String getWinner() {
        return players.values().stream()
                .max(Comparator.comparingInt(Player::getScore))
                .map(Player::getName)
                .orElse("No winner");
    }

    public String getLeaderboardString() {
        StringBuilder sb = new StringBuilder();
        players.values().stream()
                .sorted((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()))
                .forEach(p -> {
                    sb.append(p.getName()).append(",").append(p.getScore())
                            .append(",").append(p.isStopped()).append(";");
                });
        return sb.toString();
    }

    public boolean isPlayerStopped(String playerName) {
        return stoppedPlayers.contains(playerName);
    }
}
