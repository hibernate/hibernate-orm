/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.persister.entity;

import org.hibernate.persister.entity.DiscriminatorMetadata;
import org.hibernate.sql.SelectFragment;

/**
 * Extends the generic <tt>EntityPersister</tt> contract to add
 * operations required by the Hibernate Query Language
 *
 * @author Gavin King
 */
public interface Queryable extends Loadable, PropertyMapping, Joinable {

	/**
	 * Is this an abstract class?
	 */
	public boolean isAbstract();
	/**
	 * Is this class explicit polymorphism only?
	 */
	public boolean isExplicitPolymorphism();
	/**
	 * Get the class that this class is mapped as a subclass of -
	 * not necessarily the direct superclass
	 */
	public String getMappedSuperclass();
	/**
	 * Get the discriminator value for this particular concrete subclass,
	 * as a string that may be embedded in a select statement
	 */
	public String getDiscriminatorSQLValue();

	/**
	 * Given a query alias and an identifying suffix, render the intentifier select fragment.
	 */
	public String identifierSelectFragment(String name, String suffix);
	/**
	 * Given a query alias and an identifying suffix, render the property select fragment.
	 */
	public String propertySelectFragment(String alias, String suffix, boolean allProperties);

	public SelectFragment propertySelectFragmentFragment(String alias, String suffix, boolean allProperties);
	/**
	 * Get the names of columns used to persist the identifier
	 */
	public String[] getIdentifierColumnNames();

	/**
	 * Is the inheritence hierarchy described by this persister contained across
	 * multiple tables?
	 *
	 * @return True if the inheritence hierarchy is spread across multiple tables; false otherwise.
	 */
	public boolean isMultiTable();

	/**
	 * Get the names of all tables used in the hierarchy (up and down) ordered such
	 * that deletes in the given order would not cause contraint violations.
	 *
	 * @return The ordered array of table names.
	 */
	public String[] getConstraintOrderedTableNameClosure();

	/**
	 * For each table specified in {@link #getConstraintOrderedTableNameClosure()}, get
	 * the columns that define the key between the various hierarchy classes.
	 * <p/>
	 * The first dimension here corresponds to the table indexes returned in
	 * {@link #getConstraintOrderedTableNameClosure()}.
	 * <p/>
	 * The second dimension should have the same length across all the elements in
	 * the first dimension.  If not, that'd be a problem ;)
	 *
	 * @return
	 */
	public String[][] getContraintOrderedTableKeyColumnClosure();

	/**
	 * Get the name of the temporary table to be used to (potentially) store id values
	 * when performing bulk update/deletes.
	 *
	 * @return The appropriate temporary table name.
	 */
	public String getTemporaryIdTableName();

	/**
	 * Get the appropriate DDL command for generating the temporary table to
	 * be used to (potentially) store id values when performing bulk update/deletes.
	 *
	 * @return The appropriate temporary table creation command.
	 */
	public String getTemporaryIdTableDDL();

	/**
	 * Given a property name, determine the number of the table which contains the column
	 * to which this property is mapped.
	 * <p/>
	 * Note that this is <b>not</b> relative to the results from {@link #getConstraintOrderedTableNameClosure()}.
	 * It is relative to the subclass table name closure maintained internal to the persister (yick!).
	 * It is also relative to the indexing used to resolve {@link #getSubclassTableName}...
	 *
	 * @param propertyPath The name of the property.
	 * @return The nunber of the table to which the property is mapped.
	 */
	public int getSubclassPropertyTableNumber(String propertyPath);

	/**
	 * Determine whether the given property is declared by our
	 * mapped class, our super class, or one of our subclasses...
	 * <p/>
	 * Note: the method is called 'subclass property...' simply
	 * for consistency sake (e.g. {@link #getSubclassPropertyTableNumber}
	 *
	 * @param propertyPath The property name.
	 * @return The property declarer
	 */
	public Declarer getSubclassPropertyDeclarer(String propertyPath);

	/**
	 * Get the name of the table with the given index from the internal
	 * array.
	 *
	 * @param number The index into the internal array.
	 * @return
	 */
	public String getSubclassTableName(int number);

	/**
	 * Is the version property included in insert statements?
	 */
	public boolean isVersionPropertyInsertable();

	/**
	 * The alias used for any filter conditions (mapped where-fragments or
	 * enabled-filters).
	 * </p>
	 * This may or may not be different from the root alias depending upon the
	 * inheritence mapping strategy.
	 *
	 * @param rootAlias The root alias
	 * @return The alias used for "filter conditions" within the where clause.
	 */
	public String generateFilterConditionAlias(String rootAlias);

	/**
	 * Retrieve the information needed to properly deal with this entity's discriminator
	 * in a query.
	 *
	 * @return The entity discriminator metadata
	 */
	public DiscriminatorMetadata getTypeDiscriminatorMetadata();

	public static class Declarer {
		public static final Declarer CLASS = new Declarer( "class" );
		public static final Declarer SUBCLASS = new Declarer( "subclass" );
		public static final Declarer SUPERCLASS = new Declarer( "superclass" );
		private final String name;
		public Declarer(String name) {
			this.name = name;
		}
		public String toString() {
			return name;
		}
	}
}
