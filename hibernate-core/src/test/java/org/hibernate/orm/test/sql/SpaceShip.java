/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 * @author Vlad MIhalcea
 */
//tag::sql-composite-key-entity-associations_named-query-example[]
@Entity
@NamedNativeQueries({
	@NamedNativeQuery(name = "find_all_spaceships",
		query =
			"SELECT " +
			"   name as \"name\", " +
			"   model, " +
			"   speed, " +
			"   lname as lastn, " +
			"   fname as firstn, " +
			"   length, " +
			"   width, " +
			"   length * width as surface, " +
			"   length * width * 10 as volume " +
			"FROM SpaceShip",
		resultSetMapping = "spaceship"
)
})
@SqlResultSetMapping(
	name = "spaceship",
	entities = @EntityResult(
		entityClass = SpaceShip.class,
		fields = {
			@FieldResult(name = "name", column = "name"),
			@FieldResult(name = "model", column = "model"),
			@FieldResult(name = "speed", column = "speed"),
			@FieldResult(name = "captain.id.lastname", column = "lastn"),
			@FieldResult(name = "captain.id.firstname", column = "firstn"),
			@FieldResult(name = "dimensions.length", column = "length"),
			@FieldResult(name = "dimensions.width", column = "width"),
		}
),
	columns = {
		@ColumnResult(name = "surface"),
		@ColumnResult(name = "volume")
	}
)
public class SpaceShip {

	@Id
	private String name;

	private String model;

	private double speed;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fname", referencedColumnName = "firstname")
	@JoinColumn(name = "lname", referencedColumnName = "lastname")
	private Captain captain;

	private Dimensions dimensions;

	//Getters and setters are omitted for brevity

//end::sql-composite-key-entity-associations_named-query-example[]

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Captain getCaptain() {
		return captain;
	}

	public void setCaptain(Captain captain) {
		this.captain = captain;
	}

	public Dimensions getDimensions() {
		return dimensions;
	}

	public void setDimensions(Dimensions dimensions) {
		this.dimensions = dimensions;
	}
//tag::sql-composite-key-entity-associations_named-query-example[]
}
//end::sql-composite-key-entity-associations_named-query-example[]
