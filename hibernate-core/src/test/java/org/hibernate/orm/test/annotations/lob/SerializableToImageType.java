/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.lob;
import java.io.Serializable;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.type.descriptor.jdbc.LongVarbinaryJdbcType;

/**
 * A type that maps JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY} and {@link Serializable}.
 * </p>
 * TODO : this should really have access to the actual Serializable class so we have access to the proper classloader
 *
 * @author Strong Liu
 */
public class SerializableToImageType extends AbstractSingleColumnStandardBasicType<Serializable> {
	public static final PrimitiveCharacterArrayTextType INSTANCE = new PrimitiveCharacterArrayTextType();

	public SerializableToImageType() {
		super( LongVarbinaryJdbcType.INSTANCE, new SerializableJavaType<Serializable>( Serializable.class ) );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
