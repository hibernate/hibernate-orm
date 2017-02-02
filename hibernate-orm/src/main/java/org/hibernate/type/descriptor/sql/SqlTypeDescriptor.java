/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.io.Serializable;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for the <tt>SQL</tt>/<tt>JDBC</tt> side of a value mapping.
 * <p/>
 * NOTE : Implementations should be registered with the {@link SqlTypeDescriptor}.  The built-in Hibernate
 * implementations register themselves on construction.
 *
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptor extends Serializable {
	/**
	 * Return the {@linkplain java.sql.Types JDBC type-code} for the column mapped by this type.
	 *
	 * @return typeCode The JDBC type-code
	 */
	public int getSqlType();

	/**
	 * Is this descriptor available for remapping?
	 *
	 * @return {@code true} indicates this descriptor can be remapped; otherwise, {@code false}
	 *
	 * @see org.hibernate.type.descriptor.WrapperOptions#remapSqlTypeDescriptor
	 * @see org.hibernate.dialect.Dialect#remapSqlTypeDescriptor
	 */
	public boolean canBeRemapped();

	/**
	 * Get the binder (setting JDBC in-going parameter values) capable of handling values of the type described by the
	 * passed descriptor.
	 *
	 * @param javaTypeDescriptor The descriptor describing the types of Java values to be bound
	 *
	 * @return The appropriate binder.
	 */
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor);

	/**
	 * Get the extractor (pulling out-going values from JDBC objects) capable of handling values of the type described
	 * by the passed descriptor.
	 *
	 * @param javaTypeDescriptor The descriptor describing the types of Java values to be extracted
	 *
	 * @return The appropriate extractor
	 */
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor);
}
