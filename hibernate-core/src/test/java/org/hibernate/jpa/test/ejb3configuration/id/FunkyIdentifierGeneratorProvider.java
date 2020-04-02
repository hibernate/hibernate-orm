/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration.id;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 */
public class FunkyIdentifierGeneratorProvider implements org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider {
	public Map<String, Class<?>> getStrategies() {
		final HashMap<String, Class<?>> result = new HashMap<String, Class<?>>( 1 );
		result.put( "funky", FunkyIdGenerator.class );
		return result;
	}
}
