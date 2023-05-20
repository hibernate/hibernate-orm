/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerDatabaseSnapshotImpl;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * A load plan for loading an array of state by a single restrictive part.
 *
 * @author Christian Beikov
 */
public class SingleIdArrayLoadPlan extends SingleIdLoadPlan<Object[]> {

	public SingleIdArrayLoadPlan(
			EntityMappingType entityMappingType,
			ModelPart restrictivePart,
			SelectStatement sqlAst,
			JdbcParametersList jdbcParameters,
			LockOptions lockOptions,
			SessionFactoryImplementor sessionFactory) {
		super( entityMappingType, restrictivePart, sqlAst, jdbcParameters, lockOptions, sessionFactory );
	}

	@Override
	protected RowTransformer<Object[]> getRowTransformer() {
		return RowTransformerDatabaseSnapshotImpl.instance();
	}

}
