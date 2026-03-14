package aram.kocharyan.skillswap;

public class AppUser {

    public String name;
    public String surname;
    public String email;
    public String skillTeach;
    public String skillStudy;
    public String mode;      // "online" or "offline"
    public String country;   // for offline
    public String city;      // for offline

    public AppUser() {}

    public AppUser(String name, String surname, String email,
                   String skillTeach, String skillStudy,
                   String mode, String country, String city) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.skillTeach = skillTeach;
        this.skillStudy = skillStudy;
        this.mode = mode;
        this.country = country;
        this.city = city;
    }
}