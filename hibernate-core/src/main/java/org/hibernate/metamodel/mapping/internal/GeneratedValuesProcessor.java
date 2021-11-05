/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.MetamodelUnsupportedOperationException;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.GeneratedValueResolver;
import org.hibernate.metamodel.mapping.InDatabaseGeneratedValueResolver;
import org.hibernate.metamodel.mapping.StateArrayContributorMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.tuple.GenerationTiming;

/**
 * @author Steve Ebersole
 */
@Incubating
public class GeneratedValuesProcessor {
	private final SelectStatement selectStatement;
	private final List<GeneratedValueDescriptor> valueDescriptors = new ArrayList<>();
	private final List<JdbcParameter> jdbcParameters = new ArrayList<>();

	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	public GeneratedValuesProcessor(
			EntityMappingType entityDescriptor,
			GenerationTiming timing,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;

		// NOTE: we only care about db-generated values here. in-memory generation
		// is applied before the insert/update happens.

		final List<StateArrayContributorMapping> generatedValuesToSelect = new ArrayList<>();
		entityDescriptor.visitAttributeMappings( (attr) -> {
			//noinspection RedundantClassCall
			if ( ! StateArrayContributorMapping.class.isInstance( attr ) ) {
				return;
			}

			if ( attr.getValueGeneration().getGenerationTiming() == GenerationTiming.NEVER ) {
				return;
			}

			final GeneratedValueResolver generatedValueResolver = GeneratedValueResolver.from(
					attr.getValueGeneration(),
					timing,
					generatedValuesToSelect.size()
			);

			//noinspection RedundantClassCall
			if ( ! InDatabaseGeneratedValueResolver.class.isInstance( generatedValueResolver ) ) {
				// again, we only care about in in-db generations here
				return;
			}

			// this attribute is generated for the timing we are processing...
			valueDescriptors.add( new GeneratedValueDescriptor( generatedValueResolver, (StateArrayContributorMapping) attr ) );
			generatedValuesToSelect.add( (StateArrayContributorMapping) attr );
		});

		if ( generatedValuesToSelect.isEmpty() ) {
			selectStatement = null;
		}
		else {
			selectStatement = LoaderSelectBuilder.createSelect(
					entityDescriptor,
					generatedValuesToSelect,
					entityDescriptor.getIdentifierMapping(),
					null,
					1,
					LoadQueryInfluencers.NONE,
					LockOptions.READ,
					jdbcParameters::add,
					sessionFactory
			);
		}
	}

	public void processGeneratedValues(Object entity, Object id, Object[] state, SharedSessionContractImplementor session) {
		if ( selectStatement == null ) {
			return;
		}

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParamBindings.registerParametersForEachJdbcValue(
				id,
				Clause.WHERE,
				entityDescriptor.getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();
		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory
				.buildSelectTranslator( sessionFactory, selectStatement )
				.translate( jdbcParamBindings, QueryOptions.NONE );

		final List<Object[]> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return QueryOptions.NONE;
					}

					@Override
					public String getQueryIdentifier(String sql) {
						return sql;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						throw new MetamodelUnsupportedOperationException( "Follow-on locking not supported yet" );
					}

				},
				(row) -> row,
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		assert results.size() == 1;
		final Object[] dbSelectionResults = results.get( 0 );

		for ( int i = 0; i < valueDescriptors.size(); i++ ) {
			final GeneratedValueDescriptor descriptor = valueDescriptors.get( i );
			final Object generatedValue = descriptor.resolver.resolveGeneratedValue( dbSelectionResults, entity, session );
			state[ descriptor.attribute.getStateArrayPosition() ] = generatedValue;
			descriptor.attribute.getAttributeMetadataAccess()
					.resolveAttributeMetadata( entityDescriptor )
					.getPropertyAccess()
					.getSetter()
					.set( entity, generatedValue, sessionFactory );
		}
	}

	private static class GeneratedValueDescriptor {
		public final GeneratedValueResolver resolver;
		public final StateArrayContributorMapping attribute;

		public GeneratedValueDescriptor(GeneratedValueResolver resolver, StateArrayContributorMapping attribute) {
			this.resolver = resolver;
			this.attribute = attribute;
		}
	}
}
