//$Id$
package org.hibernate.test.annotations.reflection;


/**
 * @author Emmanuel Bernard
 */
public class BusTripPk {
	private String busNumber;
	private String busDriver;

	public String getBusDriver() {
		return busDriver;
	}

	public void setBusDriver(String busDriver) {
		this.busDriver = busDriver;
	}

	public String getBusNumber() {
		return busNumber;
	}

	public void setBusNumber(String busNumber) {
		this.busNumber = busNumber;
	}
}
