/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.reflection;


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
