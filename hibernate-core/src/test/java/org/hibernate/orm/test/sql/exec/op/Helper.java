/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.op;

import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class Helper {

	static SelectByIdAst createSelectByIdAst(
			EntityPersister entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		final MutableObject<JdbcParameter> jdbcParamRef = new MutableObject<>();
		final SelectStatement selectAst = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				null,
				entityDescriptor.getIdentifierMapping(),
				null,
				1,
				new LoadQueryInfluencers( sessionFactory ),
				null,
				jdbcParamRef::setIfNot,
				sessionFactory
		);

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( 1 );
		jdbcParameterBindings.addBinding(
				jdbcParamRef.get(),
				new JdbcParameterBindingImpl(
						entityDescriptor.getIdentifierMapping().getJdbcMapping( 0 ),
						1
				)
		);

		return new SelectByIdAst( selectAst, jdbcParameterBindings );
	}

	static SelectByIdQuery createSelectByIdQuery(
			EntityPersister entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		return createSelectByIdQuery( entityDescriptor, List.of(), sessionFactory );
	}

	static SelectByIdQuery createSelectByIdQuery(
			EntityPersister entityDescriptor,
			List<String> associationsToFetch,
			SessionFactoryImplementor sessionFactory) {
		final List<ModelPart> modelPartsToFetch = resolveModelPartsToFetch( entityDescriptor, associationsToFetch );

		final MutableObject<JdbcParameter> jdbcParamRef = new MutableObject<>();
		final SelectStatement selectAst = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				modelPartsToFetch,
				entityDescriptor.getIdentifierMapping(),
				null,
				1,
				new LoadQueryInfluencers( sessionFactory ),
				null,
				jdbcParamRef::setIfNot,
				sessionFactory
		);

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( 1 );
		jdbcParameterBindings.addBinding(
				jdbcParamRef.get(),
				new JdbcParameterBindingImpl(
						entityDescriptor.getIdentifierMapping().getJdbcMapping( 0 ),
						1
				)
		);
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcOperationQuerySelect jdbcOperation = jdbcServices
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, selectAst )
				.translate( jdbcParameterBindings, QueryOptions.NONE );

		return new SelectByIdQuery( jdbcOperation, selectAst, jdbcParameterBindings );
	}

	private static List<ModelPart> resolveModelPartsToFetch(
			EntityPersister entityDescriptor,
			List<String> associationsToFetch) {
		if ( CollectionHelper.isEmpty( associationsToFetch ) ) {
			return null;
		}

		final List<ModelPart> partsToFetch = new ArrayList<>();
		partsToFetch.add( entityDescriptor.getIdentifierMapping() );
		entityDescriptor.forEachAttributeMapping( (attrDescriptor) -> {
			if ( attrDescriptor.getMappedFetchOptions().getTiming() == FetchTiming.IMMEDIATE
					|| associationsToFetch.contains( attrDescriptor.getAttributeName() ) ) {
				partsToFetch.add( attrDescriptor );
			}
		} );
		return partsToFetch;
	}

	record SelectByIdAst(SelectStatement sqlAst, JdbcParameterBindings jdbcParameterBindings) {
	}

	record SelectByIdQuery(
			JdbcOperationQuerySelect jdbcOperation,
			SelectStatement sqlAst,
			JdbcParameterBindings jdbcParameterBindings) {
	}
}
