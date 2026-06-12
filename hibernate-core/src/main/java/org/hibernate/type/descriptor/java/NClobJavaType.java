/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.proxy.NClobProxy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.ResourceRegistry;
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
			final ResourceRegistry resourceRegistry = options.getResourceRegistry();
			final LobCreator lobCreator = options.getLobCreator();

			final NClob result;
			if ( value instanceof NClob clob ) {
				result = lobCreator.wrap( clob );
			}
			else if ( value instanceof String string ) {
				result = lobCreator.createNClob( string );
			}
			else if ( value instanceof Reader reader ) {
				result = lobCreator.createNClob( extractString( reader ) );
			}
			else if ( value instanceof CharacterStream stream ) {
				result = lobCreator.createNClob( stream.asReader(), stream.getLength() );
			}
			else {
				throw unknownWrap( value.getClass() );
			}
			final ReleasableNClob releasableClob = new ReleasableNClob( resourceRegistry, result );
			resourceRegistry.register( releasableClob );
			return result;
		}
	}

	public record ReleasableNClob(ResourceRegistry resourceRegistry, NClob nClob) implements NClob {

		@Override
		public long length() throws SQLException {
			return nClob.length();
		}

		@Override
		public String getSubString(long pos, int length) throws SQLException {
			return nClob.getSubString( pos, length );
		}

		@Override
		public Reader getCharacterStream() throws SQLException {
			return nClob.getCharacterStream();
		}

		@Override
		public InputStream getAsciiStream() throws SQLException {
			return nClob.getAsciiStream();
		}

		@Override
		public long position(String searchstr, long start) throws SQLException {
			return nClob.position( searchstr, start );
		}

		@Override
		public long position(Clob searchstr, long start) throws SQLException {
			return nClob.position( searchstr, start );
		}

		@Override
		public int setString(long pos, String str) throws SQLException {
			return nClob.setString( pos, str );
		}

		@Override
		public int setString(long pos, String str, int offset, int len) throws SQLException {
			return nClob.setString( pos, str, offset, len );
		}

		@Override
		public OutputStream setAsciiStream(long pos) throws SQLException {
			return nClob.setAsciiStream( pos );
		}

		@Override
		public Writer setCharacterStream(long pos) throws SQLException {
			return nClob.setCharacterStream( pos );
		}

		@Override
		public void truncate(long len) throws SQLException {
			nClob.truncate( len );
		}

		@Override
		public void free() throws SQLException {
			try {
				doFree();
			}
			finally {
				resourceRegistry.release( this );
			}
		}

		public void doFree() throws SQLException {
			nClob.free();
		}

		@Override
		public Reader getCharacterStream(long pos, long length) throws SQLException {
			return nClob.getCharacterStream( pos, length );
		}
	}
}
