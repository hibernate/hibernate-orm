/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dirtiness;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Thing {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( strategy = "increment", name = "increment" )
	private Long id;

	private String name;
	private Date mutableProperty;

	public Thing() {
	}

	public Thing(String name) {
		this.name = name;
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
		// intentionally simple dirty tracking (i.e. no checking against previous state)
		changedValues.put( "name", this.name );
		this.name = name;
	}

	public Date getMutableProperty() {
		return mutableProperty;
	}

	public void setMutableProperty(Date mutableProperty) {
		this.mutableProperty = mutableProperty;
	}

	@Transient
	Map<String,Object> changedValues = new HashMap<String, Object>();
}
