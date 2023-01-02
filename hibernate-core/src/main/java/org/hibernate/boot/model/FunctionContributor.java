/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.boot.model;

/**
 * On object that contributes custom types and type descriptors, eventually
 * to a {@link org.hibernate.query.sqm.function.SqmFunctionRegistry}, via an
 * instance of {@link FunctionContributions}.
 * <p>
 * The most common way to integrate a {@code FunctionContributor} is by making
 * it discoverable via the Java {@link java.util.ServiceLoader} facility.
 *
 * @apiNote Unfortunately there's currently no programmatic way to register
 *          an instance with {@code Configuration} or {@code MetadataBuilder}.
 *          Nor can it be registered via a corresponding setting defined in
 *          {@link org.hibernate.jpa.boot.spi.JpaSettings}. These are things
 *          which <em>are</em> possible for its best friend {@link TypeContributor}.
 *
 * @see org.hibernate.query.sqm.function.SqmFunctionRegistry
 *
 * @author Karel Maesen
 */
public interface FunctionContributor {

	/**
	 *  Contribute functions
	 *
	 * @param functionContributions The target for the contributions
	 */
	void contributeFunctions(FunctionContributions functionContributions);

	/**
	 * Determines order in which the contributions will be applied (lowest ordinal first).
	 *
	 * The range 0-500 is reserved for Hibernate, range 500-1000 for libraries and 1000-Integer.MAX_VALUE for
	 * user-defined FunctionContributors.
	 *
	 * @return the ordinal for this FunctionContributor
	 */
	default int ordinal(){
		return 1000;
	}
}
