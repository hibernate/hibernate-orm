/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.FilterDef;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
@Entity
@FilterDef(name = "nameIsNull", defaultCondition = "name is null")
public class Silly implements Serializable {
	@Id
	@GeneratedValue(generator = "increment")
	private Long id;
	private String name;
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn
	private Other other;

	public Silly() {
	}

	public Silly(String name) {
		this.name = name;
	}

	public Silly(String name, Other other) {
		this.name = name;
		this.other = other;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Other getOther() {
		return other;
	}

	public void setOther(Other other) {
		this.other = other;
	}
}
