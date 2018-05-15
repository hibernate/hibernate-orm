/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

/**
 * Specialization of BasicType for the purpose of defining the Java type of the
 * true/false value on the JDBC side (aka the "unwrap" type as determined by
 * {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor#unwrap}).
 * <p/>
 *  E.g. for a BooleanBasicType that maps a boolean to database CHAR column, the
 * unwrap type would be Character.
 *
 * @param <U> The "unwrap" type of the boolean value.
 *
 * @author Steve Ebersole
 */
public interface BooleanBasicType<U> extends BasicType<Boolean> {
	/**
	 * Returns the value that is used to represent {@code TRUE} in the db
	 */
	U getTrueValue();

	/**
	 * Returns the value that is used to represent {@code FALSE} in the db
	 */
	U getFalseValue();
}
