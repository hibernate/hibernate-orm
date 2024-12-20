/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.generationmappings;
import jakarta.persistence.Entity;

/**
 * @author Christian Beikov
 */
@Entity
public class TPCAutoEntity1 extends AbstractTPCAutoEntity {
	private String name1;

	public String getName1() {
		return name1;
	}

	public void setName1(String name1) {
		this.name1 = name1;
	}
}
