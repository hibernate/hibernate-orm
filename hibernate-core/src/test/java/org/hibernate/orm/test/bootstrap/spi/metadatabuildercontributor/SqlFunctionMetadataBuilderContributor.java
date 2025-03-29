/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.metadatabuildercontributor;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Vlad Mihalcea
 */
//tag::bootstrap-jpa-compliant-MetadataBuilderContributor-example[]
public class SqlFunctionMetadataBuilderContributor
		implements MetadataBuilderContributor {

	@Override
	public void contribute(MetadataBuilder metadataBuilder) {
		metadataBuilder.applySqlFunction(
			"instr", new StandardSQLFunction( "instr", StandardBasicTypes.INTEGER )
		);
	}
}
//end::bootstrap-jpa-compliant-MetadataBuilderContributor-example[]
