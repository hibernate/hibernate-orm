package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerArrayImpl;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * A load plan for loading an array of state by a single restrictive part.
 *
 * @author Christian Beikov
 */
public class SingleIdArrayLoadPlan extends SingleIdLoadPlan<Object[]> {

	public SingleIdArrayLoadPlan(
			@Nonnull EntityMappingType entityMappingType,
			@Nonnull ModelPart restrictivePart,
			@Nonnull SelectStatement sqlAst,
			@Nonnull JdbcParametersList jdbcParameters,
			@Nonnull LockOptions lockOptions,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		super( entityMappingType, restrictivePart, sqlAst, jdbcParameters, lockOptions, sessionFactory );
	}

	@Nonnull
	@Override
	protected RowTransformer<Object[]> getRowTransformer() {
		return RowTransformerArrayImpl.instance();
	}

}
