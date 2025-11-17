/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SqlResultSetMapping(name = "implicit",
		entities = @EntityResult(entityClass = SpaceShip.class))
@NamedNativeQueries({
@NamedNativeQuery(name = "implicitSample", query = "select * from SpaceShip", resultSetMapping = "implicit"),
@NamedNativeQuery(name = "compositekey",
		query = "select name, model, speed, lname as lastn, fname as firstn, length, width, length * width as surface, length * width *10 as volume from SpaceShip",
		resultSetMapping = "compositekey")
		})
//we're missins @SqlREsultSetMappings so look at Captain
public class SpaceShip {
	private String name;
	private String model;
	private double speed;
	private Captain captain;
	private Dimensions dimensions;

	@Id
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fname", referencedColumnName = "firstname")
	@JoinColumn(name = "lname", referencedColumnName = "lastname")
	public Captain getCaptain() {
		return captain;
	}

	public void setCaptain(Captain captain) {
		this.captain = captain;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public Dimensions getDimensions() {
		return dimensions;
	}

	public void setDimensions(Dimensions dimensions) {
		this.dimensions = dimensions;
	}
}
