/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Encapsulates definition of the underlying data structure backing a
 * sequence-style generator.
 *
 * @author Steve Ebersole
 */
public interface DatabaseStructure extends ExportableProducer {
	/**
	 * The physical name of the database structure (table or sequence).
	 * <p>
	 * Only available after {@link #registerExportables(Database)}
	 * has been called.
	 *
	 * @return The structure name.
	 */
	QualifiedName getPhysicalName();

	/**
	 * How many times has this structure been accessed through this reference?
	 * @return The number of accesses.
	 */
	int getTimesAccessed();

	/**
	 * The configured initial value
	 * @return The configured initial value
	 */
	int getInitialValue();

	/**
	 * The configured increment size
	 * @return The configured increment size
	 */
	int getIncrementSize();

	/**
	 * A callback to be able to get the next value from the underlying
	 * structure as needed.
	 *
	 * @param session The session.
	 * @return The next value.
	 */
	AccessCallback buildCallback(SharedSessionContractImplementor session);

	/**
	 * Prepare this structure for use.  Called sometime after instantiation,
	 * but before first use.
	 *
	 * @param optimizer The optimizer being applied to the generator.
	 *
	 * @deprecated Use {@link #configure(Optimizer)} instead.
	 */
	@Deprecated
	default void prepare(Optimizer optimizer) {
	}

	/**
	 * Configures this structure with the given arguments.
	 * <p>
	 * Called just after instantiation, before {@link #initialize(SqlStringGenerationContext)}
	 *
	 * @param optimizer The optimizer being applied to the generator.
	 */
	default void configure(Optimizer optimizer) {
		prepare( optimizer );
	}

	/**
	 * Register database objects involved in this structure, e.g. sequences, tables, etc.
	 * <p>
	 * This method is called just once, after {@link #configure(Optimizer)},
	 * but before {@link #initialize(SqlStringGenerationContext)}.
	 *
	 * @param database The database instance
	 */
	@Override
	void registerExportables(Database database);

	/**
	 * Initializes this structure, in particular pre-generates SQL as necessary.
	 * <p>
	 * This method is called just once, after {@link #registerExportables(Database)},
	 * before first use.
	 *
	 * @param context A context to help generate SQL strings
	 */
	default void initialize(SqlStringGenerationContext context) {
	}

	/**
	 * Is the structure physically a sequence?
	 *
	 * @return {@code true} if the actual database structure is a sequence; {@code false} otherwise.
	 */
	boolean isPhysicalSequence();

	/**
	 * @deprecated Exposed for tests only.
	 */
	@Deprecated
	public default String[] getAllSqlForTests() {
		return new String[] { };
	}
}
