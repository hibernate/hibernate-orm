/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Encapsulates definition of the underlying data structure backing a
 * sequence-style generator.
 *
 * @author Steve Ebersole
 */
public interface DatabaseStructure extends ExportableProducer {
	/**
	 * The name of the database structure (table or sequence).
	 * @return The structure name.
	 */
	String getName();

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
	 * Prepare this structure for use.  Called sometime afterQuery instantiation,
	 * but beforeQuery first use.
	 *
	 * @param optimizer The optimizer being applied to the generator.
	 */
	void prepare(Optimizer optimizer);

	/**
	 * Commands needed to create the underlying structures.
	 * @param dialect The database dialect being used.
	 * @return The creation commands.
	 */
	String[] sqlCreateStrings(Dialect dialect);

	/**
	 * Commands needed to drop the underlying structures.
	 * @param dialect The database dialect being used.
	 * @return The drop commands.
	 */
	String[] sqlDropStrings(Dialect dialect);

	/**
	 * Is the structure physically a sequence?
	 *
	 * @return {@code true} if the actual database structure is a sequence; {@code false} otherwise.
	 */
	boolean isPhysicalSequence();
}
