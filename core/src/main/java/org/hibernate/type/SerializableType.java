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
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.SerializableTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarbinaryTypeDescriptor;

/**
 * A type that maps between a {@link java.sql.Types#VARBINARY VARBINARY} and {@link Serializable} classes.
 * <p/>
 * Notice specifically the 2 forms:<ul>
 * <li>{@link #INSTANCE} indicates a mapping using the {@link Serializable} interface itself.</li>
 * <li>{@link #SerializableType(Class)} indicates a mapping using the specific class</li>
 * </ul>
 * The important distinction has to do with locating the appropriate {@link ClassLoader} to use during deserialization.
 * In the fist form we are always using the {@link ClassLoader} of the JVM (Hibernate will always fallback to trying
 * its classloader as well).  The second form is better at targeting the needed {@link ClassLoader} actually needed.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SerializableType<T extends Serializable> extends AbstractSingleColumnStandardBasicType<T> {
	public static final SerializableType<Serializable> INSTANCE = new SerializableType<Serializable>( Serializable.class );

	private final Class<T> serializableClass;

	public SerializableType(Class<T> serializableClass) {
		super( VarbinaryTypeDescriptor.INSTANCE, new SerializableTypeDescriptor<T>( serializableClass )  );
		this.serializableClass = serializableClass;
	}

	public String getName() {
		return (serializableClass==Serializable.class) ? "serializable" : serializableClass.getName();
	}
}
