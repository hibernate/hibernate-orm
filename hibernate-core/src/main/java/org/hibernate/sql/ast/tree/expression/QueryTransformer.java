/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.select.QuerySpec;

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
