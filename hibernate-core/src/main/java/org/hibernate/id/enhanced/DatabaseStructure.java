package org.hibernate.id.enhanced;

import jakarta.annotation.Nonnull;
import org.hibernate.Internal;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Table;

/**
 * Encapsulates definition of the underlying data structure backing
 * a {@linkplain SequenceStyleGenerator sequence-style} generator.
 *
 * @author Steve Ebersole
 */
public interface DatabaseStructure extends ExportableProducer {
	/**
	 * The physical name of the database structure (table or sequence).
	 *
	 * @return The structure name.
	 * @throws IllegalStateException if called before {@link #registerExportables(Database)}
	 */
	@Nonnull QualifiedName getPhysicalName();

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
	 * @return The callback that retrieves the next value.
	 */
	@Nonnull AccessCallback buildCallback(@Nonnull SharedSessionContractImplementor session);

	/**
	 * Prepare this structure for use.  Called sometime after instantiation,
	 * but before first use.
	 *
	 * @param optimizer The optimizer being applied to the generator.
	 *
	 * @deprecated Use {@link #configure(Optimizer)} instead.
	 */
	@Deprecated
	default void prepare(@Nonnull Optimizer optimizer) {
	}

	/**
	 * Configures this structure with the given arguments.
	 * <p>
	 * Called just after instantiation, before {@link #initialize(SqlStringGenerationContext)}
	 *
	 * @param optimizer The optimizer being applied to the generator.
	 */
	default void configure(@Nonnull Optimizer optimizer) {
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
	void registerExportables(@Nonnull Database database);

	/**
	 * Register additional database objects which need to be aware of the
	 * table for which this structure is used to generate values. Used to
	 * deal with automatic sequence resynchronization after data import.
	 *
	 * @param table The table for which this structure is used to generate values
	 * @param optimizer The {@link Optimizer} for this generator
	 *
	 * @see org.hibernate.relational.SchemaManager#resynchronizeGenerators()
	 *
	 * @since 7.2
	 */
	default void registerExtraExportables(@Nonnull Table table, @Nonnull Optimizer optimizer) {
	}

	/**
	 * Initializes this structure, in particular pre-generates SQL as necessary.
	 * <p>
	 * This method is called just once, after {@link #registerExportables(Database)},
	 * before first use.
	 *
	 * @param context A context to help generate SQL strings
	 */
	default void initialize(@Nonnull SqlStringGenerationContext context) {
	}

	/**
	 * Is the structure physically a sequence?
	 *
	 * @return {@code true} if the actual database structure is a sequence; {@code false} otherwise.
	 */
	boolean isPhysicalSequence();

	/**
	 * @return The SQL statements. Entries may be {@code null} before {@link #initialize(SqlStringGenerationContext)}.
	 *
	 * @deprecated Exposed for tests only.
	 */
	@Deprecated @Internal
	default @Nonnull String[] getAllSqlForTests() {
		return new String[] { };
	}
}
