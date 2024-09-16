/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestEntity extends ChildEntity {

	@Id
	long id;

	String testValue;

}
