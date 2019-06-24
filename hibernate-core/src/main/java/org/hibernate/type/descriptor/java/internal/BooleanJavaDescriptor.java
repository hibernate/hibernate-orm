/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.Primitive;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link Boolean} handling.
 *
 * @author Steve Ebersole
 */
public class BooleanJavaDescriptor extends AbstractBasicJavaDescriptor<Boolean> implements Primitive<Boolean> {
	public static final BooleanJavaDescriptor INSTANCE = new BooleanJavaDescriptor();

	public BooleanJavaDescriptor() {
		super( Boolean.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor(
				context.getPreferredSqlTypeCodeForBoolean()
		);
	}

	@Override
	public String toString(Boolean value) {
		return value == null ? null : value.toString();
	}
	@Override
	public Boolean fromString(String string) {
		return Boolean.valueOf( string );
	}

	private int toInt(Boolean value) {
		return value ? 1 : 0;
	}

	private Byte toByte(Boolean value) {
		return (byte) toInt( value );
	}

	private Short toShort(Boolean value) {
		return (short) toInt( value );
	}

	private Integer toInteger(Boolean value) {
		return toInt( value );
	}

	private Long toLong(Boolean value) {
		return (long) toInt( value );
	}

	@Override
	public Class getPrimitiveClass() {
		return boolean.class;
	}

	@Override
	public Boolean getDefaultValue() {
		return false;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Boolean value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Boolean.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) toByte( value );
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return (X) toShort( value );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) toInteger( value );
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) toLong( value );
		}
//		if ( Character.class.isAssignableFrom( type ) ) {
//			return (X) Character.valueOf( value ? characterValueTrue : characterValueFalse );
//		}
//		if ( String.class.isAssignableFrom( type ) ) {
//			return (X) (value ? stringValueTrue : stringValueFalse);
//		}
		if ( Character.class.isAssignableFrom( type ) ) {
			return (X) Character.valueOf( value ? 'T' : 'F' );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Boolean wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Boolean.class.isInstance( value ) ) {
			return (Boolean) value;
		}
		if ( Number.class.isInstance( value ) ) {
			return ( (Number) value ).intValue() == 0;
		}
		if ( Character.class.isInstance( value ) ) {
			return isTrue( (Character) value );
		}
//		if ( String.class.isInstance( value ) ) {
//			return isTrue( ( (String) value ).charAt( 0 ) ) ? TRUE : FALSE;
//		}
		if ( String.class.isInstance( value ) ) {
			return Boolean.valueOf( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}

	private boolean isTrue(char charValue) {
//		return charValue == 't' || charValue == 'T' || charValue == 'y' || charValue == 'Y';
		return charValue == 'T';
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect) {
		return 1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect) {
		return 1;
	}

	@Override
	public int getDefaultSqlScale() {
		return 0;
	}

	@Override
	public String getCheckCondition(String columnName, SqlTypeDescriptor sqlTypeDescriptor, Dialect dialect) {
		return sqlTypeDescriptor instanceof BitSqlDescriptor && !dialect.supportsBitType()
				? columnName + " in (0,1)"
				: null;
	}
}
