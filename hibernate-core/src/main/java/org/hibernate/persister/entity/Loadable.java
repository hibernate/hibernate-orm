/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.FetchMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.type.Type;

/**
 * Implemented by any {@link EntityPersister} that may be loaded
 * using a {@link org.hibernate.loader.ast.spi.Loader}.
 *
 * @author Gavin King
 *
 * @deprecated Use {@link EntityMappingType}
 */
@Deprecated(since = "6", forRemoval = true)
public interface Loadable extends EntityPersister {
	
	String ROWID_ALIAS = "rowid_";

	/**
	 * Does this persistent class have subclasses?
	 *
	 * @deprecated See {@link EntityMappingType#hasSubclasses()}
	 */
	@Deprecated
	boolean hasSubclasses();

	/**
	 * Get the discriminator type
	 */
	Type getDiscriminatorType();

	/**
	 * Get the discriminator value
	 *
	 * @deprecated Use {@link EntityMappingType#getDiscriminatorValue()} instead
	 */
	@Deprecated
	Object getDiscriminatorValue();

	/**
	 * Get the concrete subclass corresponding to the given discriminator
	 * value
	 *
	 * @deprecated Use {@link EntityDiscriminatorMapping#resolveDiscriminatorValue} instead
	 */
	@Deprecated
	String getSubclassForDiscriminatorValue(Object value);

	/**
	 * Get the names of columns used to persist the identifier
	 */
	String[] getIdentifierColumnNames();

	/**
	 * Get the result set aliases used for the identifier columns, given a suffix
	 */
	String[] getIdentifierAliases(String suffix);
	/**
	 * Get the result set aliases used for the property columns, given a suffix (properties of this class, only).
	 */
	String[] getPropertyAliases(String suffix, int i);
	
	/**
	 * Get the result set column names mapped for this property (properties of this class, only).
	 */
	String[] getPropertyColumnNames(int i);
	
	/**
	 * Get the result set aliases used for the identifier columns, given a suffix
	 */
	String getDiscriminatorAlias(String suffix);
	
	/**
	 * @return the column name for the discriminator as specified in the mapping.
	 *
	 * @deprecated Use {@link EntityDiscriminatorMapping#getSelectionExpression()} instead
	 */
	@Deprecated
	String getDiscriminatorColumnName();
	
	/**
	 * Does the result set contain rowids?
	 */
	boolean hasRowId();

	boolean isAbstract();

	/**
	 * Given a column name and the root table alias in use for the entity hierarchy, determine the proper table alias
	 * for the table in that hierarchy that contains said column.
	 *
	 * @implNote Generally speaking the column is not validated to exist. Most implementations simply return the
	 *           root alias; the exception is {@link JoinedSubclassEntityPersister}.
	 *
	 * @param columnName The column name
	 * @param rootAlias The hierarchy root alias
	 *
	 * @return The proper table alias for qualifying the given column.
	 */
	String getTableAliasForColumn(String columnName, String rootAlias);

	/**
	 * All columns to select, when loading.
	 */
	String selectFragment(String alias, String suffix);

	/**
	 * Return the column alias names used to persist/query the named property of the class or a subclass (optional operation).
	 */
	String[] getSubclassPropertyColumnAliases(String propertyName, String suffix);

	/**
	 * May this (subclass closure) property be fetched using an SQL outerjoin?
	 */
	FetchMode getFetchMode(int i);

	/**
	 * Get the type of the numbered property of the class or a subclass.
	 */
	Type getSubclassPropertyType(int i);

	/**
	 * Get the column names for the given property path
	 */
	String[] getPropertyColumnNames(String propertyPath);
}
