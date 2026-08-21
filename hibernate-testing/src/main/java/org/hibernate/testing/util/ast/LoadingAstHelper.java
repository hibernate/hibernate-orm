/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util.ast;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class LoadingAstHelper {
	/// Translation details for loading
	///
	/// @param sql The corresponding SQL
	/// @param sqlAst The corresponding SQL AST
	/// @param jdbcParameters The corresponding JDBC parameters
	public record LoaderTranslation(
			String sql,
			SelectStatement sqlAst,
			List<JdbcParameter> jdbcParameters) {
	}

	public static <I> LoaderTranslation translateLoading(
			EntityMappingType entityMappingType,
			I id,
			SessionFactoryImplementor sessionFactory) {
		return translateLoading( entityMappingType, List.of(id), sessionFactory );
	}

	public static <I> LoaderTranslation translateLoading(
			EntityMappingType entityMappingType,
			List<I> ids,
			SessionFactoryImplementor sessionFactory) {
		var jdbcParameters = new ArrayList<JdbcParameter>();
		var sqlAst = LoaderSelectBuilder.createSelect(
				entityMappingType,
				null,
				entityMappingType.getIdentifierMapping(),
				null,
				ids.size(),
				new LoadQueryInfluencers( sessionFactory ),
				LockOptions.NONE,
				jdbcParameters::add,
				sessionFactory
		);
		var sqlAstTranslator = sessionFactory
				.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst );
		var jdbcOperation = sqlAstTranslator.translate(
				buildJdbcParameterBindings( entityMappingType.getIdentifierMapping(), ids, jdbcParameters ),
				QueryOptions.NONE
		);
		return new LoaderTranslation( jdbcOperation.getSqlString(), sqlAst, jdbcParameters );
	}

	private static <I> JdbcParameterBindings buildJdbcParameterBindings(
			EntityIdentifierMapping identifierMapping,
			List<I> ids,
			ArrayList<JdbcParameter> jdbcParameters) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		identifierMapping.forEachJdbcType( (position, jdbcMapping) -> jdbcParameterBindings.addBinding(
				jdbcParameters.get( position ),
				new JdbcParameterBindingImpl( jdbcMapping, null )
		) );
		return jdbcParameterBindings;
	}

}
