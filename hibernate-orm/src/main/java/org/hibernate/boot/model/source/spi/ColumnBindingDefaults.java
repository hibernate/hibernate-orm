/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * @author Steve Ebersole
 */
public interface ColumnBindingDefaults {
	/**
	 * How should non-specification of value insertion by the individual value sources here be
	 * interpreted in terms of defaulting that value.
	 *
	 * @return {@code true} Indicates that insertions are enabled by default for all value sources which
	 * do not explicitly specify.
	 */
	boolean areValuesIncludedInInsertByDefault();

	/**
	 * How should non-specification of value updating by the individual value sources here be
	 * interpreted in terms of defaulting that value.
	 *
	 * @return {@code true} Indicates that updates are enabled by default for all value sources which
	 * do not explicitly specify.
	 */
	boolean areValuesIncludedInUpdateByDefault();

	/**
	 * How should non-specification of value nullability by the individual value sources here be
	 * interpreted in terms of defaulting that value.
	 *
	 * @return {@code true} Indicates that insertions are enabled by default for all value sources which
	 * do not explicitly specify.
	 */
	boolean areValuesNullableByDefault();
}
