/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.metadatabuildercontributor;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@JiraKey(value = "HHH-12589")
public class SqlFunctionMetadataBuilderContributorClassTest
		extends AbstractSqlFunctionMetadataBuilderContributorTest {

	@Override
	protected Object matadataBuilderContributor() {
		return SqlFunctionMetadataBuilderContributor.class;
	}
}
