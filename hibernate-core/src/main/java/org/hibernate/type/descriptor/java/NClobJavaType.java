/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Reader;
import java.io.Serializable;
import java.sql.NClob;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.proxy.NClobProxy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;

import static org.hibernate.type.descriptor.java.DataHelper.extractString;

/**
 * Descriptor for {@link NClob} handling.
 * <p>
 * Note, {@link NClob}s really are mutable (their internal state can in fact be mutated).  We simply
 * treat them as immutable because we cannot properly check them for changes nor deep copy them.
 *
 * @author Steve Ebersole
 * @author Loïc Lefèvre
 */
public class NClobJavaType extends AbstractClassJavaType<NClob> {
	public static final NClobJavaType INSTANCE = new NClobJavaType();

	public static class NClobMutabilityPlan implements MutabilityPlan<NClob> {
		public static final NClobMutabilityPlan INSTANCE = new NClobMutabilityPlan();

		public boolean isMutable() {
			return false;
		}

		public NClob deepCopy(NClob value) {
			return value;
		}

		public Serializable disassemble(NClob value, SharedSessionContract session) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}

		public NClob assemble(Serializable cached, SharedSessionContract session) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}
	}

	public NClobJavaType() {
		super( NClob.class, NClobMutabilityPlan.INSTANCE, IncomparableComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof NClob;
	}

	@Override
	public NClob cast(Object value) {
		return (NClob) value;
	}

	@Override
	public String extractLoggableRepresentation(NClob value) {
		return value == null ? "null" : "{nclob}";
	}

	public String toString(NClob value) {
		return extractString( value );
	}

	public NClob fromString(CharSequence string) {
		return NClobProxy.generateProxy( string.toString() );
	}

	@Override
	public int extractHashCode(NClob value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(NClob one, NClob another) {
		return one == another;
	}

	@Override
	public NClob getReplacement(NClob original, NClob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy()
				.mergeNClob( original, target, session );
	}

	public <X> X unwrap(final NClob value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		try {
			if ( NClob.class.isAssignableFrom( type ) ) {
				return type.cast( options.getLobCreator().toJdbcNClob( value ) );
			}
			else if ( String.class.isAssignableFrom( type ) ) {
				if (value instanceof NClobImplementer clobImplementer) {
					// if the incoming Clob is a wrapper, just get the underlying String.
					return type.cast( clobImplementer.getUnderlyingStream().asString() );
				}
				else {
					// otherwise we need to extract the String.
					return type.cast( extractString( value.getCharacterStream() ) );
				}
			}
			else if ( Reader.class.isAssignableFrom( type ) ) {
				if (value instanceof NClobImplementer clobImplementer) {
					// if the incoming NClob is a wrapper, just pass along its CharacterStream
					return type.cast( clobImplementer.getUnderlyingStream().asReader() );
				}
				else {
					// otherwise we need to build a Reader...
					return type.cast( value.getCharacterStream() );
				}
			}
			else if ( CharacterStream.class.isAssignableFrom( type ) ) {
				if (value instanceof NClobImplementer clobImplementer) {
					// if the incoming NClob is a wrapper, just pass along its CharacterStream
					return type.cast( clobImplementer.getUnderlyingStream() );
				}
				else {
					// otherwise we need to build a CharacterStream...
					return type.cast( value.getCharacterStream() );
				}
			}
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access nclob stream", e );
		}

		throw unknownUnwrap( type );
	}

	public <X> NClob wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else {
			final LobCreator lobCreator = options.getLobCreator();
			if ( value instanceof NClob clob ) {
				return lobCreator.wrap( clob );
			}
			else if ( value instanceof String string ) {
				return lobCreator.createNClob( string );
			}
			else if ( value instanceof Reader reader ) {
				return lobCreator.createNClob( extractString( reader ) );
			}
			else if ( value instanceof CharacterStream stream ) {
				return lobCreator.createNClob( stream.asReader(), stream.getLength() );
			}
			else {
				throw unknownWrap( value.getClass() );
			}
		}
	}
}
