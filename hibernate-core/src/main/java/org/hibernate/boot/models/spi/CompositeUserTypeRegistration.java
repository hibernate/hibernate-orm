/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.spi;

import org.hibernate.models.spi.ClassDetails;

/**
 * Registration for a {@linkplain org.hibernate.usertype.CompositeUserType}
 *
 * @see org.hibernate.annotations.CompositeTypeRegistration
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl
 *
 * @author Steve Ebersole
 */
public class CompositeUserTypeRegistration {
	private final ClassDetails embeddableClass;
	private final ClassDetails userTypeClass;

	public CompositeUserTypeRegistration(ClassDetails embeddableClass, ClassDetails userTypeClass) {
		this.embeddableClass = embeddableClass;
		this.userTypeClass = userTypeClass;
	}

	public ClassDetails getEmbeddableClass() {
		return embeddableClass;
	}

	public ClassDetails getUserTypeClass() {
		return userTypeClass;
	}
}
