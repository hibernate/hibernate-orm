/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.persister.entity;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;


import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.Assert.assertEquals;

@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting( name = AvailableSettings.DEFAULT_SCHEMA, value = FormulaTemplateSchemaSubstitutionTest.CUSTOM_SCHEMA),
		@Setting( name = AvailableSettings.HBM2DDL_CREATE_SCHEMAS, value = "true")
})
public class FormulaTemplateSchemaSubstitutionTest extends AbstractSchemaSubstitutionFormulaTest {

	static final String CUSTOM_SCHEMA = "CUSTOM_SCHEMA";

	@Override
	void validate(String formula) {
		assertEquals( "Formula should not contain {} characters",
					  4, formula.split( CUSTOM_SCHEMA + ".", -1 ).length - 1
		);
	}
}
