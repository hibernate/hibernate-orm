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
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.proxy.ClobProxy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.ResourceRegistry;
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
			final var result = createLob( value, options );

			if ( options.getDialect().supportsUnboundedLobLocatorMaterialization() ) {
				return result;
			}
			final var resourceRegistry = options.getResourceRegistry();
			final var releasableClob = new ReleasableClob( resourceRegistry, result );
			resourceRegistry.register( releasableClob );
			return releasableClob;
		}
	}

	private <X> Clob createLob(X value, WrapperOptions options) {
		final var lobCreator = options.getLobCreator();

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

	public record ReleasableClob(ResourceRegistry resourceRegistry, Clob clob) implements Clob {
		@Override
		public long length() throws SQLException {
			return clob.length();
		}

		@Override
		public String getSubString(long pos, int length) throws SQLException {
			return clob.getSubString( pos, length );
		}

		@Override
		public Reader getCharacterStream() throws SQLException {
			return clob.getCharacterStream();
		}

		@Override
		public InputStream getAsciiStream() throws SQLException {
			return clob.getAsciiStream();
		}

		@Override
		public long position(String searchstr, long start) throws SQLException {
			return clob.position( searchstr, start );
		}

		@Override
		public long position(Clob searchstr, long start) throws SQLException {
			return clob.position( searchstr, start );
		}

		@Override
		public int setString(long pos, String str) throws SQLException {
			return clob.setString( pos, str );
		}

		@Override
		public int setString(long pos, String str, int offset, int len) throws SQLException {
			return clob.setString( pos, str, offset, len );
		}

		@Override
		public OutputStream setAsciiStream(long pos) throws SQLException {
			return clob.setAsciiStream( pos );
		}

		@Override
		public Writer setCharacterStream(long pos) throws SQLException {
			return clob.setCharacterStream( pos );
		}

		@Override
		public void truncate(long len) throws SQLException {
			clob.truncate( len );
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
			clob.free();
		}

		@Override
		public Reader getCharacterStream(long pos, long length) throws SQLException {
			return clob.getCharacterStream( pos, length );
		}
	}
}
