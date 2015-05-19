/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import java.util.Map;

/**
 * Provide a set of IdentifierGenerator strategies allowing to override the Hibernate Core default ones
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface IdentifierGeneratorStrategyProvider {
	/**
	 * set of strategy / generator class pairs to register as accepted strategies
	 */
	public Map<String,Class<?>> getStrategies();
}
