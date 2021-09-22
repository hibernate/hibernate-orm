/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.internal.QueryLiteralHelper;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link Types#VARCHAR VARCHAR} and {@link String}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StringType
		extends AbstractSingleColumnStandardBasicType<String>
		implements DiscriminatorType<String>, AdjustableBasicType<String> {

	public static final StringType INSTANCE = new StringType();

	public StringType() {
		super( VarcharTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "string";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	public String objectToSQLString(String value, Dialect dialect) throws Exception {
		return QueryLiteralHelper.toStringLiteral( value );
	}

	public String stringToObject(String xml) throws Exception {
		return xml;
	}

	public String toString(String value) {
		return value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> BasicType<X> resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<X> domainJtd) {
		if ( ! indicators.isLob() && ! indicators.isNationalized() ) {
			return (BasicType<X>) this;
		}

		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();
		final int jdbcTypeCode;

		if ( indicators.isLob() ) {
			jdbcTypeCode = indicators.isNationalized()
					? Types.NCLOB
					: Types.CLOB;
		}
		else {
			jdbcTypeCode = indicators.isNationalized()
					? Types.NVARCHAR
					: Types.VARCHAR;
		}

		return typeConfiguration.getBasicTypeRegistry().resolve(
				domainJtd,
				jdbcTypeRegistry.getDescriptor( jdbcTypeCode ),
				getName()
		);
	}
}
