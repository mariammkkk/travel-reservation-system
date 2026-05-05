package travel;

import java.util.List;

import javax.swing.table.DefaultTableModel;

import service.BookingService.FlightLeg;

/** Table rows and parallel multi-leg keys for booking. */
public record FlightSearchResult(DefaultTableModel model, List<List<FlightLeg>> itineraries) {}
