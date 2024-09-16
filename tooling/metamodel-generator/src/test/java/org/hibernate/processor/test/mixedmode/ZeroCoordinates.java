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
public class ZeroCoordinates {
	public float getLatitude() {
		return 0f;
	}

	public void setLatitude(float latitude) {
	}

	public float getLongitude() {
		return 0f;
	}

	public void setLongitude(float longitude) {
	}
}
