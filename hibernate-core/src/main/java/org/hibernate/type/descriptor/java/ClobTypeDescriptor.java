/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.java;

import java.io.Reader;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.ClobProxy;
import org.hibernate.engine.jdbc.WrappedClob;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Clob} handling.
 * <p/>
 * Note, {@link Clob clobs} really are mutable (their internal state can in fact be mutated).  We simply
 * treat them as immutable because we cannot properly check them for changes nor deep copy them.
 *
 * @author Steve Ebersole
 */
public class ClobTypeDescriptor extends AbstractTypeDescriptor<Clob> {
	public static final ClobTypeDescriptor INSTANCE = new ClobTypeDescriptor();

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

	public ClobTypeDescriptor() {
		super( Clob.class, ClobMutabilityPlan.INSTANCE );
	}

	public String toString(Clob value) {
		return DataHelper.extractString( value );
	}

	public Clob fromString(String string) {
		return ClobProxy.generateProxy( string );
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
	public <X> X unwrap(final Clob value, Class<X> type, WrapperOptions options) {
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
					return (X) new CharacterStreamImpl( DataHelper.extractString( value.getCharacterStream() ) );
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

	public <X> Clob wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		// Support multiple return types from
		// org.hibernate.type.descriptor.sql.ClobTypeDescriptor
		if ( Clob.class.isAssignableFrom( value.getClass() ) ) {
			return options.getLobCreator().wrap( (Clob) value );
		}
		else if ( Reader.class.isAssignableFrom( value.getClass() ) ) {
			Reader reader = (Reader) value;
			return options.getLobCreator().createClob( DataHelper.extractString( reader ) );
		}

		throw unknownWrap( value.getClass() );
	}
}
