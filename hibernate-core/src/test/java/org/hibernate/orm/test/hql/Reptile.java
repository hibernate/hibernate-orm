/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * @author Gavin King
 *
 * @deprecated Use {@link org.hibernate.testing.orm.domain.animal.Reptile} instead
 */
@Deprecated
public class Reptile extends Animal {
	private float bodyTemperature;
	public float getBodyTemperature() {
		return bodyTemperature;
	}
	public void setBodyTemperature(float bodyTemperature) {
		this.bodyTemperature = bodyTemperature;
	}
}
