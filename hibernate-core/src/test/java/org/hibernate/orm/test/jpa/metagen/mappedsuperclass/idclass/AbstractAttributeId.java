/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.idclass;

import java.io.Serializable;
import jakarta.persistence.MappedSuperclass;


/**
 * @author Alexis Bataille
 * @author Steve Ebersole
 */
@MappedSuperclass
public abstract class AbstractAttributeId implements Serializable {
	protected String key;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

}
