/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class ParentEntity {

	String parentValue;

}
