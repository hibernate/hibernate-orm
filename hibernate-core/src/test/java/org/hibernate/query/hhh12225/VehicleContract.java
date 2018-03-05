/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12225;

import java.util.ArrayList;
import java.util.List;

public class VehicleContract extends Contract {
	public static final long serialVersionUID = 1L;

	private List<Vehicle> vehicles;

	public VehicleContract() {
		vehicles = new ArrayList<Vehicle>();
	}

	public void addVehicle(Vehicle vehicle) {
		vehicle.setContract( this );
		getVehicles().add( vehicle );
	}

	public String toString() {
		return String.valueOf( getId() );
	}

	public List<Vehicle> getVehicles() {
		return vehicles;
	}

	public void setVehicles(List<Vehicle> vehicles) {
		this.vehicles = vehicles;
	}


}
