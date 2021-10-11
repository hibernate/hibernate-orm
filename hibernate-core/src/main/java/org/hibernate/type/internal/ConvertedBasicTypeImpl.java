/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Christian Beikov
 */
public class ConvertedBasicTypeImpl<J> extends NamedBasicTypeImpl<J> implements ConvertedBasicType<J> {

	private final BasicValueConverter<J, ?> converter;

	public ConvertedBasicTypeImpl(
			JavaType<J> jtd,
			JdbcTypeDescriptor std,
			String name,
			BasicValueConverter<J, ?> converter) {
		super( jtd, std, name );
		this.converter = converter;
	}

	@Override
	public BasicValueConverter<J, ?> getValueConverter() {
		return converter;
	}
}
