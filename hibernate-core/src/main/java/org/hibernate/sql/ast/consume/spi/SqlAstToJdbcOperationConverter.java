/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import java.util.Set;

import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;

/**
 * Base contract for {@link SqlAstWalker} implementations that convert
 * SQL AST into a {@link JdbcOperation}
 *
 * @author Steve Ebersole
 */
public interface SqlAstToJdbcOperationConverter
		extends SqlAstWalker, JdbcRecommendedSqlTypeMappingContext {
	Set<String> getAffectedTableNames();
}
