/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.type.Type;

/**
 * An <tt>IdentifierGenerator</tt> that supports "configuration".
 *
 * @see IdentifierGenerator
 * @author Gavin King
 */
public interface Configurable {
	/**
	 * Configure this instance, given the value of parameters
	 * specified by the user as <tt>&lt;param&gt;</tt> elements.
	 * This method is called just once, following instantiation.
	 *
	 * @param params param values, keyed by parameter name
	 */
	public void configure(Type type, Properties params, JdbcEnvironment jdbcEnvironment) throws MappingException;

}
