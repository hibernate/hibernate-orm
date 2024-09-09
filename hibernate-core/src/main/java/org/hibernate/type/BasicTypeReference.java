/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.query.BindingContext;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.SqmExpressible;

/**
 * A basic type reference.
 *
 * @author Christian Beikov
 */
public final class BasicTypeReference<T> implements BindableType<T>, Serializable {
	private final String name;
	private final Class<T> javaType;
	private final int sqlTypeCode;
	private final BasicValueConverter<T, ?> converter;
	private final boolean forceImmutable;

	public BasicTypeReference(String name, Class<? extends T> javaType, int sqlTypeCode) {
		this(name, javaType, sqlTypeCode, null);
	}

	public BasicTypeReference(
			String name,
			Class<? extends T> javaType,
			int sqlTypeCode,
			BasicValueConverter<T, ?> converter) {
		this( name, javaType, sqlTypeCode, converter, false );
	}

	private BasicTypeReference(
			String name,
			Class<? extends T> javaType,
			int sqlTypeCode,
			BasicValueConverter<T, ?> converter,
			boolean forceImmutable) {
		this.name = name;
		//noinspection unchecked
		this.javaType = (Class<T>) javaType;
		this.sqlTypeCode = sqlTypeCode;
		this.converter = converter;
		this.forceImmutable = forceImmutable;
	}

	public String getName() {
		return name;
	}

	@Override
	public Class<T> getBindableJavaType() {
		return javaType;
	}

	public int getSqlTypeCode() {
		return sqlTypeCode;
	}

	public BasicValueConverter<T, ?> getConverter() {
		return converter;
	}

	public boolean isForceImmutable() {
		return forceImmutable;
	}

	public BasicTypeReference<T> asImmutable() {
		return forceImmutable ? this : new BasicTypeReference<>(
				"imm_" + name,
				javaType,
				sqlTypeCode,
				converter,
				true
		);
	}

	@Override
	public SqmExpressible<T> resolveExpressible(BindingContext bindingContext) {
		return bindingContext.getTypeConfiguration().getBasicTypeRegistry().resolve( this );
	}
}
