/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.spi;

/**
 * Support for basic-value conversions.
 *
 * Conversions might be defined by:
 *
 * 		* a custom JPA {@link javax.persistence.AttributeConverter},
 * 		* implicitly, based on the Java type (e.g., enums)
 * 	    * etc
 *
 * @author Steve Ebersole
 */
public interface BasicValueConverter<O,R> {
	/**
	 * Convert the relational form just retrieved from JDBC ResultSet into
	 * the domain form.
	 */
	O toDomainValue(R relationalForm);

	/**
	 * Convert the domain form into the relational form in preparation for
	 * storage into JDBC
	 */
	R toRelationalValue(O domainForm);
}
