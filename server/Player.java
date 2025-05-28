public class Player {
    private String name;
    private int score;
    private boolean stopped;
    
    public Player(String name) {
        this.name = name;
        this.score = 0;
        this.stopped = false;
    }
    
    public String getName() {
        return name;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    public boolean isStopped() {
        return stopped;
    }
    
    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }
}
