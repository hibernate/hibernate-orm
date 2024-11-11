/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
@Embeddable
public class Weight extends Measurement {
	private float weight;
}
