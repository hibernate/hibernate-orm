/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

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
