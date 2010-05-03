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

import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BlobProxy;
import org.hibernate.engine.jdbc.WrappedBlob;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Blob} handling.
 * <p/>
 * Note, {@link Blob blobs} really are mutable (their internal state can in fact be mutated).  We simply
 * treat them as immutable because we cannot properly check them for changes nor deep copy them.
 *
 * @author Steve Ebersole
 */
public class BlobTypeDescriptor extends AbstractTypeDescriptor<Blob> {
	public static final BlobTypeDescriptor INSTANCE = new BlobTypeDescriptor();

	public static class BlobMutabilityPlan implements MutabilityPlan<Blob> {
		public static final BlobMutabilityPlan INSTANCE = new BlobMutabilityPlan();

		public boolean isMutable() {
			return false;
		}

		public Blob deepCopy(Blob value) {
			return value;
		}

		public Serializable disassemble(Blob value) {
			throw new UnsupportedOperationException( "Blobs are not cacheable" );
		}

		public Blob assemble(Serializable cached) {
			throw new UnsupportedOperationException( "Blobs are not cacheable" );
		}
	}

	public BlobTypeDescriptor() {
		super( Blob.class, BlobMutabilityPlan.INSTANCE );
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString(Blob value) {
		final byte[] bytes;
		try {
			bytes = DataHelper.extractBytes( value.getBinaryStream() );
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access blob stream", e );
		}
		return PrimitiveByteArrayTypeDescriptor.INSTANCE.toString( bytes );
	}

	/**
	 * {@inheritDoc}
	 */
	public Blob fromString(String string) {
		return BlobProxy.generateProxy( PrimitiveByteArrayTypeDescriptor.INSTANCE.fromString( string ) );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Comparator<Blob> getComparator() {
		return IncomparableComparator.INSTANCE;
	}

	@Override
	public int extractHashCode(Blob value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(Blob one, Blob another) {
		return one == another;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Blob value, Class<X> type, WrapperOptions options) {
		if ( !Blob.class.isAssignableFrom( type ) ) {
			throw unknownUnwrap( type );
		}

		if ( value == null ) {
			return null;
		}

		final Blob blob =  WrappedBlob.class.isInstance( value )
				? ( (WrappedBlob) value ).getWrappedBlob()
				: value;
		return (X) blob;
	}

	public <X> Blob wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( ! Blob.class.isAssignableFrom( value.getClass() ) ) {
			throw unknownWrap( value.getClass() );
		}

		return options.getLobCreator().wrap( (Blob) value );
	}
}
