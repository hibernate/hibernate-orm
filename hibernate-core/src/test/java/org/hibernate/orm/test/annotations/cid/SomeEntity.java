/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author bartek
 */
@Entity
public class SomeEntity
		implements Serializable {

	@Id
	private SomeEntityId id;

	@Basic
	private String prop;

	/**
	 * @return the id
	 */
	public SomeEntityId getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(SomeEntityId id) {
		this.id = id;
	}

	/**
	 * @return the prop
	 */
	public String getProp() {
		return prop;
	}

	/**
	 * @param prop the prop to set
	 */
	public void setProp(String prop) {
		this.prop = prop;
	}

}
