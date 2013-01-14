/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.io.Serializable;
import java.sql.NClob;
import java.util.Comparator;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.NClobProxy;
import org.hibernate.engine.jdbc.WrappedNClob;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link java.sql.NClob} handling.
 * <p/>
 * Note, {@link java.sql.NClob nclobs} really are mutable (their internal state can in fact be mutated).  We simply
 * treat them as immutable because we cannot properly check them for changes nor deep copy them.
 *
 * @author Steve Ebersole
 */
public class NClobTypeDescriptor extends AbstractTypeDescriptor<NClob> {
	public static final NClobTypeDescriptor INSTANCE = new NClobTypeDescriptor();

	public static class NClobMutabilityPlan implements MutabilityPlan<NClob> {
		public static final NClobMutabilityPlan INSTANCE = new NClobMutabilityPlan();

		public boolean isMutable() {
			return false;
		}

		public NClob deepCopy(NClob value) {
			return value;
		}

		public Serializable disassemble(NClob value) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}

		public NClob assemble(Serializable cached) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}
	}

	public NClobTypeDescriptor() {
		super( NClob.class, NClobMutabilityPlan.INSTANCE );
	}

	public String toString(NClob value) {
		return DataHelper.extractString( value );
	}

	public NClob fromString(String string) {
		return NClobProxy.generateProxy( string );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Comparator<NClob> getComparator() {
		return IncomparableComparator.INSTANCE;
	}

	@Override
	public int extractHashCode(NClob value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(NClob one, NClob another) {
		return one == another;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(final NClob value, Class<X> type, WrapperOptions options) {
		if ( ! ( NClob.class.isAssignableFrom( type ) || CharacterStream.class.isAssignableFrom( type ) ) ) {
			throw unknownUnwrap( type );
		}

		if ( value == null ) {
			return null;
		}

		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			if ( NClobImplementer.class.isInstance( value ) ) {
				// if the incoming Clob is a wrapper, just pass along its CharacterStream
				return (X) ( (NClobImplementer) value ).getUnderlyingStream();
			}
			else {
				// otherwise we need to build one...
				return (X) new CharacterStreamImpl( DataHelper.extractString( value ) );
			}
		}

		final NClob clob =  WrappedNClob.class.isInstance( value )
				? ( (WrappedNClob) value ).getWrappedNClob()
				: value;
		return (X) clob;
	}

	public <X> NClob wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( ! NClob.class.isAssignableFrom( value.getClass() ) ) {
			throw unknownWrap( value.getClass() );
		}

		return options.getLobCreator().wrap( (NClob) value );
	}
}
