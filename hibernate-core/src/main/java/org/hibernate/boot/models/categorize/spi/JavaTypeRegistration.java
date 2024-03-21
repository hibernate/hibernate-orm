/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.ClassDetails;

/**
 * {@linkplain org.hibernate.type.descriptor.java.JavaType} registration
 *
 * @see org.hibernate.annotations.JavaTypeRegistration
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbJavaTypeRegistrationImpl
 *
 * @author Steve Ebersole
 */
public class JavaTypeRegistration {
	private final ClassDetails domainType;
	private final ClassDetails descriptor;

	public JavaTypeRegistration(ClassDetails domainType, ClassDetails descriptor) {
		this.domainType = domainType;
		this.descriptor = descriptor;
	}

	public ClassDetails getDomainType() {
		return domainType;
	}

	public ClassDetails getDescriptor() {
		return descriptor;
	}
}
