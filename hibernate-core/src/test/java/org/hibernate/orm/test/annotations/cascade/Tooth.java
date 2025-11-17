/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Tooth {
	@Id
	@GeneratedValue
	public Integer id;
	@Column(name = "`type`")
	public String type;
	@ManyToOne(cascade = CascadeType.PERSIST)
	public Tooth leftNeighbour;
	@ManyToOne(cascade = CascadeType.MERGE)
	public Tooth rightNeighbour;
	@ManyToOne
	public Mouth mouth;
}
