/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;
import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.Ingres10IdentityColumnSupport;

/**
 * A SQL dialect for Ingres 10 and later versions.
 * <p/>
 * Changes:
 * <ul>
 * <li>Add native BOOLEAN type support</li>
 * <li>Add identity column support</li>
 * </ul>
 *
 * @author Raymond Fan
 */
public class Ingres10Dialect extends Ingres9Dialect {
	/**
	 * Constructs a Ingres10Dialect
	 */
	public Ingres10Dialect() {
		super();
		registerBooleanSupport();
		registerDefaultProperties();
	}

	protected void registerBooleanSupport() {
		// Boolean type (mapping/BooleanType) mapping maps SQL BIT to Java
		// Boolean. In order to create a boolean column, BIT needs to be mapped
		// to boolean as well, similar to H2Dialect.
		registerColumnType( Types.BIT, "boolean" );
		registerColumnType( Types.BOOLEAN, "boolean" );
	}

	private void registerDefaultProperties() {
		// true, false and unknown are now valid values
		// Remove the query substitutions previously added in IngresDialect.
		final Properties properties = getDefaultProperties();
		final String querySubst = properties.getProperty( Environment.QUERY_SUBSTITUTIONS );
		if ( querySubst != null ) {
			final String newQuerySubst = querySubst.replace( "true=1,false=0", "" );
			properties.setProperty( Environment.QUERY_SUBSTITUTIONS, newQuerySubst );
		}
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String toBooleanValueString(boolean bool) {
		return bool ? "true" : "false";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new Ingres10IdentityColumnSupport();
	}

}
