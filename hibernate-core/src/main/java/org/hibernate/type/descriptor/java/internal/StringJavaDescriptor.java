/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Types;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link String} handling.
 *
 * @author Steve Ebersole
 */
public class StringJavaDescriptor extends AbstractBasicJavaDescriptor<String> {
	public static final StringJavaDescriptor INSTANCE = new StringJavaDescriptor();

	public StringJavaDescriptor() {
		super( String.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		final int jdbcTypeCode;
		if ( context.isNationalized() && context.isLob() ) {
			jdbcTypeCode = Types.NCLOB;
		}
		else if ( context.isLob() ) {
			jdbcTypeCode = Types.CLOB;
		}
		else if ( context.isNationalized() ) {
			jdbcTypeCode = Types.NVARCHAR;
		}
		else {
			jdbcTypeCode = Types.VARCHAR;
		}

		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( jdbcTypeCode );
	}

	public String toString(String value) {
		return value;
	}

	public String fromString(String string) {
		return string;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(String value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( value );
		}
		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			return (X) new CharacterStreamImpl( value );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) session.getLobCreator().createClob( value );
		}
		if ( LobStreamDataHelper.isNClob( type ) ) {
			return (X) session.getLobCreator().createNClob( value );
		}

		throw unknownUnwrap( type );
	}

	public <X> String wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return (String) value;
		}
		if ( Reader.class.isInstance( value ) ) {
			return LobStreamDataHelper.extractString( (Reader) value );
		}
		if ( Clob.class.isInstance( value ) ) {
			return LobStreamDataHelper.extractString( (Clob) value );
		}

		throw unknownWrap( value.getClass() );
	}
}
