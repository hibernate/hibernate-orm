/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.Embeddable;

@Embeddable
public class MapElement {
	private String name;
}
