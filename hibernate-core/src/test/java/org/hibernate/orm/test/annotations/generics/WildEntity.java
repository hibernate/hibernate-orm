/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class WildEntity implements Serializable {

	private static final long serialVersionUID = -1171578628576139205L;

	private int id;

	private String property;

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	@Transient
	public <T extends Object> T someMethod() {
		return null;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}
}
