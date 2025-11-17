/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.deep;

import jakarta.persistence.Basic;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A {@link Plane} subclass entity that defines extra attributes
 *
 * @author Igor Vaynberg
 */
@Entity
@DiscriminatorValue("JetPlane")
public class JetPlane extends Plane {
	@Basic(optional = false)
	private Integer jets;

}
