/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;

import java.io.Serializable;
import java.lang.InstantiationException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Provides alternative types for binding LOB values.
 *
 * @author Gail Badner
 */
public abstract class AlternativeLobTypes<S, T extends LobType<S>> implements Serializable {

	private final T defaultType;
	private final T streamBindingType;
	private final T lobBindingType;

	private AlternativeLobTypes(Class<? extends T> clazz,
								SqlTypeDescriptor defaultDescriptor,
								SqlTypeDescriptor streamBindingDescriptor,
								SqlTypeDescriptor lobBindingDescriptor) {
		Constructor constructor = getConstructor( clazz );
		defaultType = createLobType( clazz, constructor, defaultDescriptor );
		streamBindingType = createLobType( clazz, constructor, streamBindingDescriptor );
		lobBindingType = createLobType( clazz, constructor, lobBindingDescriptor );
	}

	/**
	 * Returns the type that uses the default binding LOB values.

	 * @return type that uses the default binding
	 * @see BlobTypeDescriptor#DEFAULT
	 * @see ClobTypeDescriptor#DEFAULT
	 */
	public final T getDefaultType() {
		return defaultType;
	}

	/**
	 * Returns the type that binds LOB values using streams.
	 *
	 * @return type that binds using a stream
	 *
	 * @see BlobTypeDescriptor#STREAM_BINDING
	 * @see ClobTypeDescriptor#STREAM_BINDING
	 */
	public final T getStreamBindingType() {
		return streamBindingType;
	}

	/**
	 * Returns the type that explicitly binds the LOB value,

	 * @return type that binds the LOB
	 * @see BlobTypeDescriptor#BLOB_BINDING
	 * @see ClobTypeDescriptor#CLOB_BINDING
	 */
	public final T getLobBindingType() {
		return lobBindingType;
	}

	protected Constructor getConstructor(Class<? extends T> clazz) {
		try {
			return  clazz.getDeclaredConstructor( SqlTypeDescriptor.class, this.getClass() );
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException(
					"Could not get constructor for " +
							 clazz.getClass().getName() +
							" with argument types: [" +
							SqlTypeDescriptor.class.getName() + ", " + this.getClass().getName() +
							"]", e
			);
		}
	}

	protected T createLobType(Class<? extends T> lobTypeClass,
							  Constructor constructor,
							  SqlTypeDescriptor sqlTypeDescriptor) {
		try {
			return lobTypeClass.cast( constructor.newInstance( sqlTypeDescriptor, this ) );
		}
		catch ( InstantiationException e ) {
			throw new HibernateException( "Cannot instantiate type: " + lobTypeClass.getName() );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "IllegalAccessException trying to instantiate: " + lobTypeClass.getName() );
		}
		catch (InvocationTargetException e) {
			throw new HibernateException( "Could not create type: " + lobTypeClass.getName(), e.getCause() );
		}
	}

	/**
	 * Provides alternative types for binding {@link java.sql.Types#CLOB CLOB} values.
 	 */
	public static class ClobTypes<S, T extends LobType<S>> extends AlternativeLobTypes<S,T> {

		/* package-protected */
		ClobTypes(Class<? extends T> clobTypeClass) {
			super(
					clobTypeClass,
					ClobTypeDescriptor.DEFAULT,
					ClobTypeDescriptor.STREAM_BINDING,
					ClobTypeDescriptor.CLOB_BINDING
			);
		}
	}

	/**
	 * Provides alternative types for binding {@link java.sql.Types#BLOB BLOB} values.
 	 */
	public static class BlobTypes<S, T extends LobType<S>> extends AlternativeLobTypes<S,T> {

		private final T primitiveArrayBindingType;

		/* package-protected */
		BlobTypes(Class<? extends T> blobTypeClass) {
			super(
					blobTypeClass,
					BlobTypeDescriptor.DEFAULT,
					BlobTypeDescriptor.STREAM_BINDING,
					BlobTypeDescriptor.BLOB_BINDING
			);
			Constructor constructor = getConstructor( blobTypeClass );
			primitiveArrayBindingType = createLobType(
					blobTypeClass, constructor, BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING
			);
		}

		/**
		 * Returns the type that explicitly binds the {@link java.sql.Types#BLOB BLOB} value,

		 * @return type that binds the {@link java.sql.Types#BLOB BLOB}
		 * @see BlobTypeDescriptor#PRIMITIVE_ARRAY_BINDING
		 */
		public final T getPrimitiveArrayBindingType() {
			return primitiveArrayBindingType;
		}
	}
 }
