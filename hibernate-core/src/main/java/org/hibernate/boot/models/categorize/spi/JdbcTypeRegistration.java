/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.ClassDetails;

/**
 * {@linkplain org.hibernate.type.descriptor.jdbc.JdbcType} registration
 *
 * @see org.hibernate.annotations.JdbcTypeRegistration
 * @see org.hibernate.boot.jaxb.mapping.JaxbJdbcTypeRegistration
 *
 * @author Steve Ebersole
 */
public class JdbcTypeRegistration {
	private final Integer code;
	private final ClassDetails descriptor;

	public JdbcTypeRegistration(Integer code, ClassDetails descriptor) {
		this.code = code;
		this.descriptor = descriptor;
	}

	public Integer getCode() {
		return code;
	}

	public ClassDetails getDescriptor() {
		return descriptor;
	}
}
