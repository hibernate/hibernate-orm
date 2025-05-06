/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh12225;

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
