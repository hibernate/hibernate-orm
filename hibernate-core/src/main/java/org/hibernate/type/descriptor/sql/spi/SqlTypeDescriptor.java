/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Describes a JDBC/SQL type.
 *
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptor extends org.hibernate.type.descriptor.sql.SqlTypeDescriptor {
	/**
	 * Is this descriptor available for remapping?
	 * <p/>
	 * Mainly this comes into play as part of Dialect SqlTypeDescriptor remapping,
	 * which is how we handle LOB binding e.g. But some types should not allow themselves
	 * to be remapped.
	 *
	 * @return {@code true} indicates this descriptor can be remapped; otherwise, {@code false}
	 *
	 * @see WrapperOptions#remapSqlTypeDescriptor
	 * @see org.hibernate.dialect.Dialect#remapSqlTypeDescriptor
	 * @see org.hibernate.dialect.Dialect#getSqlTypeDescriptorOverride
	 */
	boolean canBeRemapped();

	/**
	 * Get the JavaTypeDescriptor for the Java type recommended by the JDBC spec for mapping the
	 * given JDBC/SQL type.  The standard implementations honor the JDBC recommended mapping as per
	 * http://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html
	 *
	 * @param typeConfiguration Access to Hibernate's current TypeConfiguration (type information)
	 *
	 * @return the recommended Java type descriptor.
	 */
	<T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration);

	/**
	 * todo (6.0) : move to JdbcValueMapper
	 */
	<T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor);

	<T> SqlExpressableType getSqlExpressableType(
			BasicJavaDescriptor<T> javaTypeDescriptor,
			TypeConfiguration typeConfiguration);
}
