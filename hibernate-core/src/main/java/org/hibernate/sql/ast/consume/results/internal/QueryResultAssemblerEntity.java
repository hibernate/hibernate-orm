/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.results.internal;

import java.sql.SQLException;

import org.hibernate.sql.ast.produce.result.internal.QueryResultEntityImpl;
import org.hibernate.sql.ast.consume.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.ast.consume.results.spi.RowProcessingState;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;

/**
 * @author Steve Ebersole
 */
public class QueryResultAssemblerEntity implements QueryResultAssembler {
	private final QueryResultEntityImpl returnEntity;

	public QueryResultAssemblerEntity(QueryResultEntityImpl returnEntity) {
		this.returnEntity = returnEntity;
	}

	@Override
	public Class getReturnedJavaType() {
		return returnEntity.getReturnedJavaType();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		return returnEntity.getInitializer().getEntityInstance();
	}
}
