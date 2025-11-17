/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.GroupedSchemaValidatorImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10332")
@RequiresDialect(H2Dialect.class)
public class GroupedSchemaValidatorImplTest extends IndividuallySchemaValidatorImplTest {
	@Override
	protected void getSchemaValidator(MetadataImplementor metadata) {
		new GroupedSchemaValidatorImpl( tool, DefaultSchemaFilter.INSTANCE )
				.doValidation( metadata, executionOptions, ContributableMatcher.ALL );
	}
}
