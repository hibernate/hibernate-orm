/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.SerializableJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarbinarySqlDescriptor;

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
public class SerializableType<T extends Serializable> extends BasicTypeImpl<T> {
	public static final SerializableType<Serializable> INSTANCE = new SerializableType<>( Serializable.class );

	private final Class<T> serializableClass;

	public SerializableType(Class<T> serializableClass) {
		super( new SerializableJavaDescriptor<T>( serializableClass ), VarbinarySqlDescriptor.INSTANCE );
		this.serializableClass = serializableClass;
	}

	public String getName() {
		return (serializableClass==Serializable.class) ? "serializable" : serializableClass.getName();
	}

	@Override
	public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		// no literal support for binary data
		return null;
	}
}
