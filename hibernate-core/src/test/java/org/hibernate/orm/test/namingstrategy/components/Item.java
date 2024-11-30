/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.components;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class Item {
	private String name;
}
