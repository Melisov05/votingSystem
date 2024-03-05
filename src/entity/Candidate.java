package entity;

public class Candidate {
    private String name;
    private String photo;
    private int amountOfVotes;

    public Candidate(String name, String photo) {
        this.name = name;
        this.photo = photo;
        this.amountOfVotes = 0;
    }

    public String getName() {
        return name;
    }

    public String getPhoto() {
        return photo;
    }

    public int getAmountOfVotes() {
        return amountOfVotes;
    }

    public void incrementVotes(){
        this.amountOfVotes++;
    }
}
