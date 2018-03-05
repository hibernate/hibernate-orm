/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mykhaylo Gnylorybov
 */
@RequiresDialect(H2Dialect.class)
public class FormulaTemplateEmptySchemaSubstitutionTest extends AbstractSchemaSubstitutionFormulaTest {

	@Override
	void validate(String formula) {
		assertTrue( "Formula should not contain {} characters", formula.matches( "^[^{}]+$" ) );
		assertFalse( "Formula should not contain hibernate placeholder", formula.contains( SCHEMA_PLACEHOLDER ) );
	}
}
