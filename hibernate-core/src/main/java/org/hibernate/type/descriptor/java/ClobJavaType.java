/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.engine.jdbc.proxy.ClobProxy;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import static org.hibernate.type.descriptor.java.DataHelper.extractString;

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
	public boolean isInstance(Object value) {
		return value instanceof Clob;
	}

	@Override
	public Clob cast(Object value) {
		return (Clob) value;
	}

	@Override
	public String extractLoggableRepresentation(Clob value) {
		return value == null ? "null" : "{clob}";
	}

	public String toString(Clob value) {
		return extractString( value );
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

	public <X> X unwrap(final Clob value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		try {
			if ( Clob.class.isAssignableFrom( type ) ) {
				return type.cast( options.getLobCreator().toJdbcClob( value ) );
			}
			else if ( String.class.isAssignableFrom( type ) ) {
				if (value instanceof ClobImplementer clobImplementer) {
					// if the incoming Clob is a wrapper, just grab the string from its CharacterStream
					return type.cast( clobImplementer.getUnderlyingStream().asString() );
				}
				else {
					// otherwise extract the bytes from the stream manually
					return type.cast( extractString( value.getCharacterStream() ) );
				}
			}
			else if ( Reader.class.isAssignableFrom( type ) ) {
				if (value instanceof ClobImplementer clobImplementer) {
					// if the incoming NClob is a wrapper, just pass along its BinaryStream
					return type.cast( clobImplementer.getUnderlyingStream().asReader() );
				}
				else {
					// otherwise we need to build a CharacterStream...
					return type.cast( value.getCharacterStream() );
				}
			}
			else if ( CharacterStream.class.isAssignableFrom( type ) ) {
				if (value instanceof ClobImplementer clobImplementer) {
					// if the incoming Clob is a wrapper, just pass along its CharacterStream
					return type.cast( clobImplementer.getUnderlyingStream() );
				}
				else {
					// otherwise we need to build a CharacterStream...
					return type.cast( value.getCharacterStream() );
				}
			}
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access clob stream", e );
		}

		throw unknownUnwrap( type );
	}

	public <X> Clob wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else {
			final LobCreator lobCreator = options.getLobCreator();
			if ( value instanceof Clob clob ) {
				return lobCreator.wrap( clob );
			}
			else if ( value instanceof String string ) {
				return lobCreator.createClob( string );
			}
			else if ( value instanceof Reader reader ) {
				return lobCreator.createClob( extractString( reader ) );
			}
			else if ( value instanceof CharacterStream stream ) {
				return lobCreator.createClob( stream.asReader(), stream.getLength() );
			}
			else {
				throw unknownWrap( value.getClass() );
			}
		}
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
