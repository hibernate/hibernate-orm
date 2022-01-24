/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

/**
 * Extends the generic {@link EntityPersister} contract to add
 * operations required by the Hibernate Query Language
 *
 * @author Gavin King
 *
 * @deprecated See {@link org.hibernate.metamodel.mapping.Queryable}
 */
@Deprecated(since = "6.0")
public interface Queryable extends Loadable, PropertyMapping, Joinable {

	/**
	 * Is this an abstract class?
	 */
	boolean isAbstract();
	/**
	 * Is this class explicit polymorphism only?
	 */
	boolean isExplicitPolymorphism();
	/**
	 * Get the class that this class is mapped as a subclass of -
	 * not necessarily the direct superclass
	 */
	String getMappedSuperclass();
	/**
	 * Get the discriminator value for this particular concrete subclass,
	 * as a string that may be embedded in a select statement
	 */
	String getDiscriminatorSQLValue();

	/**
	 * Get the names of columns used to persist the identifier
	 */
	String[] getIdentifierColumnNames();

	/**
	 * Is the inheritance hierarchy described by this persister contained across
	 * multiple tables?
	 *
	 * @return True if the inheritance hierarchy is spread across multiple tables; false otherwise.
	 *
	 * @deprecated Use {@link EntityPersister#getSqmMultiTableMutationStrategy} instead
	 */
	@Deprecated(since = "6.0")
	boolean isMultiTable();

	/**
	 * Get the names of all tables used in the hierarchy (up and down) ordered such
	 * that deletes in the given order would not cause constraint violations.
	 *
	 * @return The ordered array of table names.
	 */
	String[] getConstraintOrderedTableNameClosure();

	/**
	 * For each table specified in {@link #getConstraintOrderedTableNameClosure()}, get
	 * the columns that define the key between the various hierarchy classes.
	 * <p/>
	 * The first dimension here corresponds to the table indexes returned in
	 * {@link #getConstraintOrderedTableNameClosure()}.
	 * <p/>
	 * The second dimension should have the same length across all the elements in
	 * the first dimension.  If not, that would be a problem ;)
	 *
	 */
	String[][] getContraintOrderedTableKeyColumnClosure();

	/**
	 * Given a property name, determine the number of the table which contains the column
	 * to which this property is mapped.
	 * <p/>
	 * Note that this is <b>not</b> relative to the results from {@link #getConstraintOrderedTableNameClosure()}.
	 * It is relative to the subclass table name closure maintained internal to the persister (yick!).
	 * It is also relative to the indexing used to resolve {@link #getSubclassTableName}...
	 *
	 * @param propertyPath The name of the property.
	 * @return The number of the table to which the property is mapped.
	 */
	int getSubclassPropertyTableNumber(String propertyPath);

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
	Declarer getSubclassPropertyDeclarer(String propertyPath);

	/**
	 * Get the name of the table with the given index from the internal
	 * array.
	 *
	 * @param number The index into the internal array.
	 */
	String getSubclassTableName(int number);

	/**
	 * Is the version property included in insert statements?
	 */
	boolean isVersionPropertyInsertable();

	/**
	 * The alias used for any filter conditions (mapped where-fragments or
	 * enabled-filters).
	 * </p>
	 * This may or may not be different from the root alias depending upon the
	 * inheritance mapping strategy.
	 *
	 * @param rootAlias The root alias
	 * @return The alias used for "filter conditions" within the where clause.
	 */
	String generateFilterConditionAlias(String rootAlias);

	/**
	 * Retrieve the information needed to properly deal with this entity's discriminator
	 * in a query.
	 *
	 * @return The entity discriminator metadata
	 */
	DiscriminatorMetadata getTypeDiscriminatorMetadata();

	String[][] getSubclassPropertyFormulaTemplateClosure();

	class Declarer {
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
