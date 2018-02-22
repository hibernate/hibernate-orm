package org.hibernate.internal.hhh12225;

import java.util.ArrayList;
import java.util.List;


/** 
 *       Walkaway Contract
 *     
*/
public class VehicleContract extends Contract {
	public static final long serialVersionUID = 1L;

    /** persistent field */
    private List<Vehicle> vehicles;

    /** default constructor */
    public VehicleContract() {
		vehicles = new ArrayList<Vehicle>();
    }

	public void addVehicle(Vehicle vehicle) {
		vehicle.setContract(this);
		getVehicles().add(vehicle);
	}

	public String toString() {
		return String.valueOf(getId());
	}

	public List<Vehicle> getVehicles() {
		return vehicles;
	}

	public void setVehicles(List<Vehicle> vehicles) {
		this.vehicles = vehicles;
	}


}
