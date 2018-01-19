/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RequiresDialect(H2Dialect.class)
public class FormulaTemplateSchemaSubstitutionTest extends AbstractSchemaSubstitutionFormulaTest {

	private static final String CUSTOM_SCHEMA = "CUSTOM_SCHEMA";

	@Override
	protected void configure(Configuration configuration) {
		final Properties properties = new Properties();
		properties.put( "hibernate.default_schema", CUSTOM_SCHEMA );
		configuration.addProperties( properties );
	}

	@Override
	protected String createSecondSchema() {
		return CUSTOM_SCHEMA;
	}

	@Override
	void validate(String formula) {
		assertEquals( "Formula should not contain {} characters",
					  4, formula.split( CUSTOM_SCHEMA + ".", -1 ).length - 1
		);
	}
}
