/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.BasicContainerType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;

/**
 * Descriptor for a Java container type.
 *
 * @author Christian Beikov
 */
public interface BasicContainerJavaTypeDescriptor<T> extends Serializable {
	/**
	 * Get the Java type descriptor for the element type
	 */
	JavaTypeDescriptor<T> getElementDescriptor();
	/**
	 * Creates a container type for the given element type
	 */
	BasicType<?> createType(BasicType<T> elementType);

}
