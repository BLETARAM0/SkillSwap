package aram.kocharyan.skillswap;

public class AppUser {

    public String name;
    public String surname;
    public String email;
    public String skillTeach;
    public String skillStudy;

    public AppUser() {}

    public AppUser(String name, String surname, String email,
                   String skillTeach, String skillStudy) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.skillTeach = skillTeach;
        this.skillStudy = skillStudy;
    }
}