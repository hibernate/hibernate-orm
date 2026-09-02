/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.spi.query.cte.CteContainer;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;

/**
 * @author Christian Beikov
 */
@Incubating
public interface QueryTransformer {

	QuerySpec transform(
			CteContainer cteContainer,
			QuerySpec querySpec,
			SqmToSqlAstConverter converter);
}
