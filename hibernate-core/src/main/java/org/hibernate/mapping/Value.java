/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A value is anything that is persisted by value, instead of by
 * reference. It is essentially a Hibernate {@link Type}, together
 * with zero or more columns. Values are wrapped by things with
 * higher level semantics, for example properties, collections,
 * classes.
 *
 * @author Gavin King
 */
public interface Value extends Serializable {

	/**
	 * The number of columns and formulas in the mapping.
	 */
	int getColumnSpan();

	/**
	 * @deprecated moving away from the use of {@link Iterator} as a return type
	 */
	@Deprecated(since = "6.0")
	Iterator<Selectable> getColumnIterator();

	/**
	 * The mapping to columns and formulas.
	 */
	List<Selectable> getSelectables();

	/**
	 * If the mapping involves only columns, return them.
	 *
	 * @throws org.hibernate.AssertionFailure if the mapping involves formulas
	 */
	List<Column> getColumns();

	/**
	 * Same as {@link #getSelectables()} except it returns the PK for the
	 * non-owning side of a one-to-one association.
	 */
	default List<Selectable> getVirtualSelectables() {
		return getSelectables();
	}

	/**
	 * Same as {@link #getColumns()} except it returns the PK for the
	 * non-owning side of a one-to-one association.
	 *
	 * @throws org.hibernate.AssertionFailure if the mapping involves formulas
	 */
	default List<Column> getConstraintColumns() {
		return getColumns();
	}

	Type getType() throws MappingException;

	FetchMode getFetchMode();

	Table getTable();

	boolean hasFormula();

	boolean isAlternateUniqueKey();

	boolean isNullable();

	void createForeignKey();
	void createUniqueKey();

	boolean isSimpleValue();

	boolean isValid(Mapping mapping) throws MappingException;

	void setTypeUsingReflection(String className, String propertyName) throws MappingException;

	Object accept(ValueVisitor visitor);

	boolean isSame(Value other);

	boolean[] getColumnInsertability();
	boolean hasAnyInsertableColumns();

	boolean[] getColumnUpdateability();
	boolean hasAnyUpdatableColumns();

	ServiceRegistry getServiceRegistry();
	Value copy();
}
