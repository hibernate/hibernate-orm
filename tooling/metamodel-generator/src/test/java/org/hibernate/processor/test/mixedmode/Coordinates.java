/*
 * SPDX-License-Identifier: Apache-2.0
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
