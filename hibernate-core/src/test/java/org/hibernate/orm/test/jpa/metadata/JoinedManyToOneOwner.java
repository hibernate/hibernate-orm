/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;

/**
 * An entity that defines a @ManyToOne @JoinTable
 * <p>
 * See HHH-4720 for details
 *
 * @author Steve Ebersole
 */
@Entity
public class JoinedManyToOneOwner {
	private Long id;
	private House house;
	private House house2;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne @JoinTable( name = "SOME_OTHER_TABLE" )
	public House getHouse() {
		return house;
	}

	public void setHouse(House house) {
		this.house = house;
	}

	@ManyToOne
	@JoinColumn(name = "house2", nullable = false)
	public House getHouse2() {
		return house2;
	}

	public void setHouse2(House house2) {
		this.house2 = house2;
	}
}
