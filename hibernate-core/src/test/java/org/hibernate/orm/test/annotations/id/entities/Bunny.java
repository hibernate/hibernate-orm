/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

/**
 * Blown precision on related entity when &#064;JoinColumn is used.
 *
 * @see ANN-748
 * @author Andrew C. Oliver andyspam@osintegrators.com
 */
@Entity
public class Bunny implements Serializable {
	@Id
	@GeneratedValue(generator = "java5_uuid")
	@GenericGenerator(name = "java5_uuid", type = org.hibernate.orm.test.annotations.id.UUIDGenerator.class)
	@Column(name = "id", precision = 128, scale = 0)
	private BigDecimal id;

	@OneToMany(mappedBy = "bunny", cascade = { CascadeType.PERSIST })
	Set<PointyTooth> teeth;

	@OneToMany(mappedBy = "bunny", cascade = { CascadeType.PERSIST })
	Set<TwinkleToes> toes;

	public void setTeeth(Set<PointyTooth> teeth) {
		this.teeth = teeth;
	}

	public BigDecimal getId() {
		return id;
	}
}
