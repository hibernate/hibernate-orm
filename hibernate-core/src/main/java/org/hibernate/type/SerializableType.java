/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

/**
 * A type that maps between a {@link java.sql.Types#VARBINARY VARBINARY} and {@link Serializable} classes.
 * <p>
 * Notice specifically the 3 constructors:<ul>
 *     <li>{@link #INSTANCE} indicates a mapping using the {@link Serializable} interface itself.</li>
 *     <li>{@link #SerializableType(Class)} indicates a mapping using the specific class</li>
 *     <li>{@link #SerializableType(JavaType)} indicates a mapping using the specific JavaType</li>
 * </ul>
 * <p>
 * The important distinction has to do with locating the appropriate {@link ClassLoader} to use during deserialization.
 * In the fist form we are always using the {@link ClassLoader} of the JVM (Hibernate will always fallback to trying
 * its classloader as well).  The second and third forms are better at targeting the needed {@link ClassLoader} actually needed.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SerializableType<T extends Serializable> extends AbstractSingleColumnStandardBasicType<T> {
	public static final SerializableType<Serializable> INSTANCE = new SerializableType<>( Serializable.class );

	private final Class<T> serializableClass;

	public SerializableType(Class<T> serializableClass) {
		super( VarbinaryJdbcType.INSTANCE, new SerializableJavaType<>( serializableClass )  );
		this.serializableClass = serializableClass;
	}

	public SerializableType(JavaType<T> jtd) {
		super( VarbinaryJdbcType.INSTANCE, jtd  );
		this.serializableClass = jtd.getJavaTypeClass();
	}

	public String getName() {
		return (serializableClass==Serializable.class) ? "serializable" : serializableClass.getName();
	}
}
