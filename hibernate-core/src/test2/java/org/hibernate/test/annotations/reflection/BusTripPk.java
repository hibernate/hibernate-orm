/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
