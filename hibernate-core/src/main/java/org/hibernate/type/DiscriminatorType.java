/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;


import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * Additional contract for a {@link Type} may be used for a discriminator.
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface DiscriminatorType<T> extends IdentifierType<T> {

	/**
	 * The descriptor for the Java type represented by this
	 * expressable type
	 */
	JavaTypeDescriptor<T> getJavaTypeDescriptor();

	/**
	 * The descriptor for the SQL type represented by this
	 * expressable type
	 */
	JdbcTypeDescriptor getJdbcTypeDescriptor();
}
