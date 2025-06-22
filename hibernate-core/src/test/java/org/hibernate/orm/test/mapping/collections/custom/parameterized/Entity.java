/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.Parameter;


/**
 * Our test entity
 *
 * @author Steve Ebersole
 */
@jakarta.persistence.Entity
public class Entity {
	private String name;
	private List values = new ArrayList();

	public Entity() {
	}

	public Entity(String name) {
		this.name = name;
	}

	@Id
	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	@ElementCollection( targetClass = String.class, fetch = FetchType.EAGER )
	@CollectionType(
			type = DefaultableListType.class,
			parameters = @Parameter(name = "default", value = "Hello" )
	)
	@CollectionTable(name = "ENTVALS", joinColumns = @JoinColumn( name = "ENT_ID" ))
	@OrderColumn( name = "POS" )
	@Column(name = "VAL")
	public List getValues() {
		return values;
	}

	public void setValues(List values) {
		this.values = values;
	}
}
