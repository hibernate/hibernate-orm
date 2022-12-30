/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.spi;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Support for {@linkplain org.hibernate.type basic-typed} value conversions.
 * <p>
 * Conversions might be determined by:
 * <ul>
 * <li>a custom JPA {@link jakarta.persistence.AttributeConverter}, or
 * <li>implicitly, based on the Java type, for example, for Java {@code enum}s.
 * </ul>
 * @param <D> The Java type we use to represent the domain (object) type
 * @param <R> The Java type we use to represent the relational type
 *
 * @author Steve Ebersole
 */
@Incubating
public interface BasicValueConverter<D,R> {
	/**
	 * Convert the relational form just retrieved from JDBC ResultSet into
	 * the domain form.
	 */
	D toDomainValue(R relationalForm);

	/**
	 * Convert the domain form into the relational form in preparation for
	 * storage into JDBC
	 */
	R toRelationalValue(D domainForm);

	/**
	 * Descriptor for the Java type for the domain portion of this converter
	 */
	JavaType<D> getDomainJavaType();

	/**
	 * Descriptor for the Java type for the relational portion of this converter
	 */
	JavaType<R> getRelationalJavaType();

	/**
	 * The check constraint that should be added to the column
	 * definition in generated DDL.
	 *
	 * @param columnName the name of the column
	 * @param sqlType the {@link JdbcType} of the mapped column
	 * @param dialect the SQL {@link Dialect}
	 * @return a check constraint condition or null
	 */
	@Incubating
	default String getCheckCondition(String columnName, JdbcType sqlType, Dialect dialect) {
		return null;
	}

	@Incubating
	default String getSpecializedTypeDeclaration(JdbcType jdbcType, Dialect dialect) {
		return null;
	}
}
