/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;


/**
 * Boat class. Mapped in a Joined manner
 *
 * @author Emmanuel Bernard
 */
@Entity()
@Inheritance(
		strategy = InheritanceType.JOINED
)
public class Boat implements Serializable {
	private Integer id;
	private int size;
	private int weight;

	public Boat() {
		super();
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	@Column(name = "boat_size")
	public int getSize() {
		return size;
	}

	public void setId(Integer integer) {
		id = integer;
	}

	public void setSize(int i) {
		size = i;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
}
