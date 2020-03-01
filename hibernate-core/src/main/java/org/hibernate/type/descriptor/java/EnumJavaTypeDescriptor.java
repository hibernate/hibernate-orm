/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import javax.persistence.EnumType;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.sql.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.TinyIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * Describes a Java Enum type.
 *
 * @author Steve Ebersole
 */
public class EnumJavaTypeDescriptor<T extends Enum<T>> extends AbstractTypeDescriptor<T> {
	@SuppressWarnings("unchecked")
	public EnumJavaTypeDescriptor(Class<T> type) {
		super( type, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		if ( context.getEnumeratedType() != null && context.getEnumeratedType() == EnumType.STRING ) {
			return context.isNationalized()
					? context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.NVARCHAR )
					: context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.VARCHAR );
		}
		else {
			return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.TINYINT );
		}
	}

	@Override
	public String toString(T value) {
		return value == null ? "<null>" : value.name();
	}

	@Override
	public T fromString(String string) {
		return string == null ? null : Enum.valueOf( getJavaType(), string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( String.class.equals( type ) ) {
			return (X) toName( value );
		}
		else if ( Integer.class.equals( type ) ) {
			return (X) toInteger( value );
		}
		else if ( Byte.class.equals( type ) ) {
			return (X) toByte( value );
		}
		return (X) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> T wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( value instanceof String ) {
			return fromName( (String) value );
		}
		else if ( value instanceof Integer ) {
			return fromInteger( (Integer) value );
		}
		else if ( value instanceof Byte ) {
			return fromByte( (Byte) value );
		}

		return (T) value;
	}

	/**
	 * Convert a value of the enum type to its ordinal value
	 */
	public Byte toByte(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return (byte) domainForm.ordinal();
	}

	/**
	 * Convert a value of the enum type to its ordinal value
	 */
	public Integer toInteger(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return domainForm.ordinal();
	}

	/**
	 * Convert a value of the enum type to its ordinal value
	 */
	public Integer toOrdinal(T domainForm) {
		return toInteger( domainForm );
	}

	/**
	 * Interpret a numeric value as the ordinal of the enum type
	 */
	public T fromByte(Byte relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return getJavaType().getEnumConstants()[ relationalForm ];
	}

	/**
	 * Interpret a numeric value as the ordinal of the enum type
	 */
	public T fromInteger(Integer relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return getJavaType().getEnumConstants()[ relationalForm ];
	}

	/**
	 * Interpret a numeric value as the ordinal of the enum type
	 */
	public T fromOrdinal(Integer relationalForm) {
		return fromInteger( relationalForm );
	}

	/**
	 * Convert a value of the enum type to its name value
	 */
	public String toName(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return domainForm.name();
	}

	/**
	 * Interpret a String value as the named value of the enum type
	 */
	public T fromName(String relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return Enum.valueOf( getJavaType(), relationalForm.trim() );
	}

	@Override
	public String getCheckCondition(String columnName, SqlTypeDescriptor sqlTypeDescriptor, Dialect dialect) {
		if (sqlTypeDescriptor instanceof TinyIntTypeDescriptor
				|| sqlTypeDescriptor instanceof IntegerTypeDescriptor) {
			int last = getJavaType().getEnumConstants().length - 1;
			return columnName + " between 0 and " + last;
		}
		else if (sqlTypeDescriptor instanceof VarcharTypeDescriptor) {
			StringBuilder types = new StringBuilder();
			for ( Enum<T> value : getJavaType().getEnumConstants() ) {
				if (types.length() != 0) {
					types.append(", ");
				}
				types.append("'").append( value.name() ).append("'");
			}
			return columnName + " in (" + types + ")";
		}
		return null;
	}
}
