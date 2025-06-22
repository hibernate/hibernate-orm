/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Soldier2 implements Serializable {
	private Integer id;
	private String name;
	private Troop2 troop;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "troop_fk")
	public Troop2 getTroop() {
		return troop;
	}

	public void setTroop(Troop2 troop) {
		this.troop = troop;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Soldier2 ) ) return false;

		final Soldier2 soldier = (Soldier2) o;

		if ( !name.equals( soldier.name ) ) return false;

		return true;
	}

	public int hashCode() {
		return name.hashCode();
	}
}
