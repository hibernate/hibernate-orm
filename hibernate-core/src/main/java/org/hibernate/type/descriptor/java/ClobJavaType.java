/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Reader;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.ClobProxy;
import org.hibernate.engine.jdbc.WrappedClob;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link Clob} handling.
 * <p>
 * Note, strictly-speaking a {@link Clob} object is actually mutable (its internal state can in fact be mutated).
 * But we treat them as immutable because we simply have no way to dirty check nor deep copy them.
 *
 * @author Steve Ebersole
 * @author Loïc Lefèvre
 */
public class ClobJavaType extends AbstractClassJavaType<Clob> {
	public static final ClobJavaType INSTANCE = new ClobJavaType();

	public ClobJavaType() {
		super( Clob.class, ClobMutabilityPlan.INSTANCE, IncomparableComparator.INSTANCE );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.isNationalized()
				? indicators.getJdbcType( Types.NCLOB )
				: super.getRecommendedJdbcType( indicators );
	}

	@Override
	public String extractLoggableRepresentation(Clob value) {
		return value == null ? "null" : "{clob}";
	}

	public String toString(Clob value) {
		return DataHelper.extractString( value );
	}

	public Clob fromString(CharSequence string) {
		return ClobProxy.generateProxy( string.toString() );
	}

	@Override
	public int extractHashCode(Clob value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(Clob one, Clob another) {
		return one == another;
	}

	@Override
	public Clob getReplacement(Clob original, Clob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy()
				.mergeClob( original, target, session );
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(final Clob value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		try {
			if ( CharacterStream.class.isAssignableFrom( type ) ) {
				if (value instanceof ClobImplementer) {
					// if the incoming Clob is a wrapper, just pass along its CharacterStream
					return (X) ( (ClobImplementer) value ).getUnderlyingStream();
				}
				else {
					// otherwise we need to build a CharacterStream...
					return (X) new CharacterStreamImpl( DataHelper.extractString( value.getCharacterStream() ) );
				}
			}
			else if ( String.class.isAssignableFrom( type ) ) {
				if (value instanceof ClobImplementer) {
					// if the incoming Clob is a wrapper, just grab the bytes from its BinaryStream
					return (X) ( (ClobImplementer) value ).getUnderlyingStream().asString();
				}
				else {
					// otherwise extract the bytes from the stream manually
					return (X) LobStreamDataHelper.extractString( value.getCharacterStream() );
				}
			}
			else if (Clob.class.isAssignableFrom( type )) {
				final Clob clob =  value instanceof WrappedClob
						? ( (WrappedClob) value ).getWrappedClob()
						: getOrCreateClob(value, options);
				return (X) clob;
			}
			else if ( String.class.isAssignableFrom( type ) ) {
				if (value instanceof ClobImplementer) {
					// if the incoming Clob is a wrapper, just get the underlying String.
					return (X) ( (ClobImplementer) value ).getUnderlyingStream().asString();
				}
				else {
					// otherwise we need to extract the String.
					return (X) DataHelper.extractString( value.getCharacterStream() );
				}
			}
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access clob stream", e );
		}
		
		throw unknownUnwrap( type );
	}

	private Clob getOrCreateClob(Clob value, WrapperOptions options) throws SQLException {
		if(options.getDialect().useConnectionToCreateLob()) {
			if(value.length() == 0) {
				// empty Clob
				return options.getLobCreator().createClob("");
			}
			else {
				return options.getLobCreator().createClob(value.getSubString(1, (int) value.length()));
			}
		}
		else {
			return value;
		}
	}

	public <X> Clob wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		// Support multiple return types from
		// org.hibernate.type.descriptor.sql.ClobTypeDescriptor
		if ( Clob.class.isAssignableFrom( value.getClass() ) ) {
			return options.getLobCreator().wrap( (Clob) value );
		}
		else if ( String.class.isAssignableFrom( value.getClass() ) ) {
			return options.getLobCreator().createClob( ( String ) value);
		}
		else if ( Reader.class.isAssignableFrom( value.getClass() ) ) {
			Reader reader = (Reader) value;
			return options.getLobCreator().createClob( DataHelper.extractString( reader ) );
		}
		else if ( String.class.isAssignableFrom( value.getClass() ) ) {
			return options.getLobCreator().createClob( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultLobLength();
	}

	/**
	 * MutabilityPlan for Clob values
	 */
	public static class ClobMutabilityPlan implements MutabilityPlan<Clob> {
		public static final ClobMutabilityPlan INSTANCE = new ClobMutabilityPlan();

		public boolean isMutable() {
			return false;
		}

		public Clob deepCopy(Clob value) {
			return value;
		}

		public Serializable disassemble(Clob value, SharedSessionContract session) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}

		public Clob assemble(Serializable cached, SharedSessionContract session) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}
	}
}
