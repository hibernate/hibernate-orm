/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations.internal;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

public class NoJdbcTypeDescriptor implements JdbcTypeDescriptor {
	@Override
	public int getJdbcTypeCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canBeRemapped() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		throw new UnsupportedOperationException();
	}
}
