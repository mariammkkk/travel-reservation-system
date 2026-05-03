public class Main {
    public static void main(String[] args) {
        FindFlights.searchFlights("EWR", "ORD", "2026-05-01", null, false, false);
        FindFlights.searchFlights("EWR", "ORD", "2026-05-01", null, false, true);
        
        SortFlights.sortFlightsDuration("ASC", "EWR", "ORD");
        SortFlights.sortFlightsTakeOff_Landing_Price("DESC", "EWR", "ORD", true, false, false); //takeoff
        SortFlights.sortFlightsTakeOff_Landing_Price("ASC", "EWR", "ORD", false, true, false); //landing
        SortFlights.sortFlightsTakeOff_Landing_Price("ASC", "EWR", "ORD", false, false, true); //price
    }
}