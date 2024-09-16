/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mixedmode;

import jakarta.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class Coordinates {
	public float longitude;
	public float latitude;
}
