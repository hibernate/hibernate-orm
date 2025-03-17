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

import org.hibernate.service.JavaServiceLoadable;

/**
 * On object that contributes custom HQL functions, eventually to a
 * {@link org.hibernate.query.sqm.function.SqmFunctionRegistry}, via an
 * instance of {@link FunctionContributions}.
 * <ul>
 * <li>
 *     The most common way to integrate a {@code FunctionContributor} is by
 *     making it discoverable via the Java {@link java.util.ServiceLoader}
 *     facility.
 * <li>
 *     Alternatively, a {@code FunctionContributor} may be programmatically supplied to
 *     {@link org.hibernate.cfg.Configuration#registerFunctionContributor(FunctionContributor)}
 *     or even {@link org.hibernate.boot.MetadataBuilder#applyFunctions(FunctionContributor)}.
 * </ul>
 *
 * @author Karel Maesen
 * @see org.hibernate.query.sqm.function.SqmFunctionRegistry
 */
@JavaServiceLoadable
public interface FunctionContributor extends Ordinated {

	/**
	 * Contribute functions
	 *
	 * @param functionContributions The target for the contributions
	 */
	void contributeFunctions(FunctionContributions functionContributions);
}
