/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.results.spi.QueryResultProducer;

/**
 * Generalized contract for any type of function reference in the query
 *
 * @author Steve Ebersole
 */
public interface Function extends Expression, SqlExpressable, QueryResultProducer {

	@Override
	AllowableFunctionReturnType getType();
}
