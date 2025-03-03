/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import org.hibernate.annotations.Type;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Paolo Perrotta
 */
@Entity
public class Gene<T, STE extends Enum> {

	private Integer id;
	private STE state;

	@Type( StateType.class )
	public STE getState() {
		return state;
	}

	public void setState(STE state) {
		this.state = state;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = DNA.class)
	public T getGeneticCode() {
		return null;
	}

	public void setGeneticCode(T gene) {
	}
}
