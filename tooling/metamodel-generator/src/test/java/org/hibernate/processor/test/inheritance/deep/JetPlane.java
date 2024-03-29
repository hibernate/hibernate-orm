/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
