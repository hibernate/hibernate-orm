/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.query;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SqlResultSetMapping(name = "implicit",
		entities = @EntityResult(entityClass = org.hibernate.test.annotations.query.SpaceShip.class))
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
	@JoinColumns({
	@JoinColumn(name = "fname", referencedColumnName = "firstname"),
	@JoinColumn(name = "lname", referencedColumnName = "lastname")
			})
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
