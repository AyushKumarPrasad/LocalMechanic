package mechanic.local.kumar.ayush.localmechanic.Model;

public class UserInformation {

    public String name;
    public double latitude;
    public double longitude;
    public String rating ;

    public UserInformation(){

    }
    public UserInformation(String name, double latitude, double longitude , String rating){
        this.name=name;
        this.latitude=latitude;
        this.longitude=longitude;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }
}