/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * @author Christian Beikov
 */
@SuppressWarnings("rawtypes")
public class NamedStandardBasicTypeImpl<J> extends StandardBasicTypeImpl<J> {

	private final String name;

	public NamedStandardBasicTypeImpl(JavaTypeDescriptor<J> jtd, JdbcTypeDescriptor std, String name) {
		super( jtd, std );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public BasicType resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor domainJtd) {
		final JdbcTypeDescriptor recommendedSqlType = getJavaTypeDescriptor().getRecommendedJdbcType( indicators );
		if ( recommendedSqlType == getJdbcTypeDescriptor() ) {
			return this;
		}

		return indicators.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( getJavaTypeDescriptor(), recommendedSqlType, getName() );
	}
}
