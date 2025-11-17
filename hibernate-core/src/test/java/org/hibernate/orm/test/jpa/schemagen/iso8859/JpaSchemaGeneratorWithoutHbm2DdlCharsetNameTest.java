/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen.iso8859;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.schemagen.JpaSchemaGeneratorTest;

import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( H2Dialect.class )
public class JpaSchemaGeneratorWithoutHbm2DdlCharsetNameTest
		extends JpaSchemaGeneratorTest {

	@Override
	public String getScriptFolderPath() {
		return super.getScriptFolderPath() + "iso8859/";
	}

	protected String encodedName() {
		return "sch" + '\uFFFD' +"magen-test";
	}
}
