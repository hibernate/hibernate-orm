/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration.id;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ejb.cfg.spi.IdentifierGeneratorStrategyProvider;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class FunkyIdentifierGeneratorProvider implements IdentifierGeneratorStrategyProvider {
	public Map<String, Class<?>> getStrategies() {
		final HashMap<String, Class<?>> result = new HashMap<String, Class<?>>( 1 );
		result.put( "funky", FunkyIdGenerator.class );
		return result;
	}
}
