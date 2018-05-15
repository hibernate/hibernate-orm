/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.Reader;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.ClobProxy;
import org.hibernate.engine.jdbc.WrappedClob;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.spi.IncomparableComparator;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link Clob} handling.
 * <p/>
 * Note, {@link Clob clobs} really are mutable (their internal state can in fact be mutated).  We simply
 * treat them as immutable because we cannot properly check them for changes nor deep copy them.
 *
 * @author Steve Ebersole
 */
public class ClobJavaDescriptor extends AbstractBasicJavaDescriptor<Clob> {
	public static final ClobJavaDescriptor INSTANCE = new ClobJavaDescriptor();

	public static class ClobMutabilityPlan implements MutabilityPlan<Clob> {
		public static final ClobMutabilityPlan INSTANCE = new ClobMutabilityPlan();

		public boolean isMutable() {
			return false;
		}

		public Clob deepCopy(Clob value) {
			return value;
		}

		public Serializable disassemble(Clob value) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}

		public Clob assemble(Serializable cached) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}
	}

	public ClobJavaDescriptor() {
		super( Clob.class, ClobMutabilityPlan.INSTANCE );
	}

	public String toString(Clob value) {
		return LobStreamDataHelper.extractString( value );
	}

	public Clob fromString(String string) {
		return ClobProxy.generateProxy( string );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		final int jdbcCode;
		if ( context.isNationalized() ) {
			jdbcCode = Types.NCLOB;
		}
		else {
			jdbcCode = Types.CLOB;
		}
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( jdbcCode );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Comparator<Clob> getComparator() {
		return IncomparableComparator.INSTANCE;
	}

	@Override
	public int extractHashCode(Clob value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(Clob one, Clob another) {
		return one == another;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(final Clob value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		try {
			if ( CharacterStream.class.isAssignableFrom( type ) ) {
				if ( ClobImplementer.class.isInstance( value ) ) {
					// if the incoming Clob is a wrapper, just pass along its CharacterStream
					return (X) ( (ClobImplementer) value ).getUnderlyingStream();
				}
				else {
					// otherwise we need to build a CharacterStream...
					return (X) new CharacterStreamImpl( LobStreamDataHelper.extractString( value.getCharacterStream() ) );
				}
			}
			else if (Clob.class.isAssignableFrom( type )) {
				final Clob clob =  WrappedClob.class.isInstance( value )
						? ( (WrappedClob) value ).getWrappedClob()
						: value;
				return (X) clob;
			}
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access clob stream", e );
		}
		
		throw unknownUnwrap( type );
	}

	public <X> Clob wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		// Support multiple return types from
		// ClobTypeDescriptor
		if ( Clob.class.isAssignableFrom( value.getClass() ) ) {
			return session.getLobCreator().wrap( (Clob) value );
		}
		else if ( Reader.class.isAssignableFrom( value.getClass() ) ) {
			Reader reader = (Reader) value;
			return session.getLobCreator().createClob( LobStreamDataHelper.extractString( reader ) );
		}

		throw unknownWrap( value.getClass() );
	}
}
