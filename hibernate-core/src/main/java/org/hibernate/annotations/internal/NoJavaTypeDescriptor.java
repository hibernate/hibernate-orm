/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations.internal;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

public class NoJavaTypeDescriptor implements BasicJavaDescriptor<Void> {
	@Override
	public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Void fromString(String string) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> X unwrap(Void value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> Void wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException();
	}
}
