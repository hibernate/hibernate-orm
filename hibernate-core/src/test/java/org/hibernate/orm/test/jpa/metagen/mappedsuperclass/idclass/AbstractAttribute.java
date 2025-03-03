/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.idclass;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * @author Alexis Bataille
 * @author Steve Ebersole
 */
@MappedSuperclass
public abstract class AbstractAttribute implements Serializable {
	protected String key;
	protected String value;

	public AbstractAttribute() {
		super();
	}

	@Transient public abstract String getOwner();

	@Transient public String getKey() { return key; }

	public void setKey(String key) {
		this.key = key;
	}

	@Column(name = "attribute_value")
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
