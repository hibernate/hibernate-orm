/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.singletable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.DiscriminatorFormula;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorColumn(discriminatorType=DiscriminatorType.INTEGER)
@DiscriminatorFormula("case when zik_type is null then 0 else zik_type end")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"avgBeat", "starred"} ))
public abstract class Music {
	private Integer id;
	private int avgBeat;
	private Integer type;

	@Column(name = "zik_type")
	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getAvgBeat() {
		return avgBeat;
	}

	public void setAvgBeat(int avgBeat) {
		this.avgBeat = avgBeat;
	}
}
