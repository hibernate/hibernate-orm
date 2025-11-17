/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.strategy;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"start_country", "start_city"}),
		indexes = @Index(name="storm_name_idx", columnList = "stormName"))
public class Storm {
	private Integer id;
	private Location start;
	private Location end;
	private String stormName;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Embedded
	public Location getStart() {
		return start;
	}

	public void setStart(Location start) {
		this.start = start;
	}

	@Embedded
	public Location getEnd() {
		return end;
	}

	public void setEnd(Location end) {
		this.end = end;
	}

	@Column(unique = true)
	public String getStormName() {
		return stormName;
	}

	public void setStormName(String name) {
		this.stormName = name;
	}
}
