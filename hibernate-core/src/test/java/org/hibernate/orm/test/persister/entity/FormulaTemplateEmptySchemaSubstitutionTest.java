/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import org.hibernate.dialect.H2Dialect;


import org.hibernate.testing.orm.junit.RequiresDialect;

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
