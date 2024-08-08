/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValueBasicResultBuilder;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.id.insert.InsertReturningDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.query.results.TableGroupImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowTransformerArrayImpl;
import org.hibernate.sql.results.jdbc.internal.DirectResultSetAccess;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesResultSetImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.type.descriptor.WrapperOptions;

import static org.hibernate.generator.internal.NaturalIdHelper.getNaturalIdPropertyNames;

/**
 * Factory and helper methods for {@link GeneratedValuesMutationDelegate} framework.
 *
 * @author Marco Belladelli
 */
@Internal
public class GeneratedValuesHelper {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( IdentifierGeneratorHelper.class );

	/**
	 * Reads the {@link EntityPersister#getGeneratedProperties(EventType) generated values}
	 * for the specified {@link ResultSet}.
	 *
	 * @param statement The prepared statement the result set was generated from
	 * @param resultSet The result set from which to extract the generated values
	 * @param persister The entity type which we're reading the generated values for
	 * @param wrapperOptions The session
	 *
	 * @return The generated values
	 *
	 * @throws SQLException Can be thrown while accessing the result set
	 * @throws HibernateException Indicates a problem reading back a generated value
	 */
	public static GeneratedValues getGeneratedValues(
			PreparedStatement statement,
			ResultSet resultSet,
			EntityPersister persister,
			EventType timing,
			WrapperOptions wrapperOptions) throws SQLException {
		if ( resultSet == null ) {
			return null;
		}

		final GeneratedValuesMutationDelegate delegate = persister.getMutationDelegate(
				timing == EventType.INSERT ? MutationType.INSERT : MutationType.UPDATE
		);
		final GeneratedValuesMappingProducer mappingProducer =
				(GeneratedValuesMappingProducer) delegate.getGeneratedValuesMappingProducer();
		final List<GeneratedValueBasicResultBuilder> resultBuilders = mappingProducer.getResultBuilders();
		final List<ModelPart> generatedProperties = new ArrayList<>( resultBuilders.size() );
		for ( GeneratedValueBasicResultBuilder resultBuilder : resultBuilders ) {
			generatedProperties.add( resultBuilder.getModelPart() );
		}

		final GeneratedValuesImpl generatedValues = new GeneratedValuesImpl( generatedProperties );
		final Object[] results = readGeneratedValues(
				statement,
				resultSet,
				persister,
				mappingProducer,
				wrapperOptions.getSession()
		);

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Extracted generated values %s: %s",
					MessageHelper.infoString( persister ),
					results
			);
		}

		for ( int i = 0; i < results.length; i++ ) {
			generatedValues.addGeneratedValue( generatedProperties.get( i ), results[i] );
		}

		return generatedValues;
	}

	/**
	 * Utility method that reads the generated values from the specified {@link ResultSet}
	 * using the {@link JdbcValuesMappingProducer} provided in input.
	 *
	 * @param statement the prepared statement that the result set was generated from
	 * @param resultSet the result set containing the generated values
	 * @param persister the current entity persister
	 * @param mappingProducer the mapping producer to use when reading generated values
	 * @param session the current session
	 *
	 * @return an object array containing the generated values, order is consistent with the generated model parts list
	 */
	private static Object[] readGeneratedValues(
			PreparedStatement statement,
			ResultSet resultSet,
			EntityPersister persister,
			JdbcValuesMappingProducer mappingProducer,
			SharedSessionContractImplementor session) {
		final ExecutionContext executionContext = new BaseExecutionContext( session );

		final DirectResultSetAccess directResultSetAccess = new DirectResultSetAccess(
				session,
				statement,
				resultSet
		);

		final JdbcValues jdbcValues = new JdbcValuesResultSetImpl(
				directResultSetAccess,
				null,
				null,
				QueryOptions.NONE,
				true,
				mappingProducer.resolve(
						directResultSetAccess,
						session.getLoadQueryInfluencers(),
						session.getSessionFactory()
				),
				null,
				executionContext
		);

		final JdbcValuesSourceProcessingOptions processingOptions = new JdbcValuesSourceProcessingOptions() {
			@Override
			public Object getEffectiveOptionalObject() {
				return null;
			}

			@Override
			public String getEffectiveOptionalEntityName() {
				return null;
			}

			@Override
			public Object getEffectiveOptionalId() {
				return null;
			}

			@Override
			public boolean shouldReturnProxies() {
				return true;
			}
		};

		final JdbcValuesSourceProcessingStateStandardImpl valuesProcessingState = new JdbcValuesSourceProcessingStateStandardImpl(
				executionContext,
				processingOptions
		);

		final RowReader<Object[]> rowReader = ResultsHelper.createRowReader(
				session.getFactory(),
				RowTransformerArrayImpl.instance(),
				Object[].class,
				jdbcValues
		);

		final RowProcessingStateStandardImpl rowProcessingState = new RowProcessingStateStandardImpl(
				valuesProcessingState,
				executionContext,
				rowReader,
				jdbcValues
		);

		final List<Object[]> results = ListResultsConsumer.<Object[]>instance( ListResultsConsumer.UniqueSemantic.NONE )
				.consume(
						jdbcValues,
						session,
						processingOptions,
						valuesProcessingState,
						rowProcessingState,
						rowReader
				);

		if ( results.isEmpty() ) {
			throw new HibernateException(
					"The database returned no natively generated values : " + persister.getNavigableRole().getFullPath()
			);
		}

		return results.get( 0 );
	}

	/**
	 * Utility method that instantiates a {@link JdbcValuesMappingProducer} so it can be cached by the
	 * {@link GeneratedValuesMutationDelegate delegates} when they are instantiated.
	 *
	 * @param persister the current entity persister
	 * @param timing the timing of the mutation operation
	 * @param supportsArbitraryValues if we should process arbitrary (non-identifier) generated values
	 * @param supportsRowId if we should process {@link org.hibernate.metamodel.mapping.EntityRowIdMapping rowid}s
	 * {@code false} if we should retrieve the index through the column expression
	 *
	 * @return the instantiated jdbc values mapping producer
	 */
	public static GeneratedValuesMappingProducer createMappingProducer(
			EntityPersister persister,
			EventType timing,
			boolean supportsArbitraryValues,
			boolean supportsRowId) {
		// This is just a mock table group needed to correctly resolve expressions
		final NavigablePath parentNavigablePath = new NavigablePath( persister.getEntityName() );
		final TableGroup tableGroup = new TableGroupImpl(
				parentNavigablePath,
				null,
				new NamedTableReference( "t", "t" ),
				persister
		);
		// Create the mapping producer and add all result builders to it
		final List<? extends ModelPart> generatedProperties = getActualGeneratedModelParts(
				persister,
				timing,
				supportsArbitraryValues,
				supportsRowId
		);
		final GeneratedValuesMappingProducer mappingProducer = new GeneratedValuesMappingProducer();
		for ( int i = 0; i < generatedProperties.size(); i++ ) {
			final ModelPart modelPart = generatedProperties.get( i );
			final BasicValuedModelPart basicModelPart = modelPart.asBasicValuedModelPart();
			if ( basicModelPart != null ) {
				final GeneratedValueBasicResultBuilder resultBuilder = new GeneratedValueBasicResultBuilder(
						parentNavigablePath.append( basicModelPart.getSelectableName() ),
						basicModelPart,
						tableGroup,
						supportsArbitraryValues ? i : null
				);
				mappingProducer.addResultBuilder( resultBuilder );
			}
			else {
				throw new UnsupportedOperationException( "Unsupported generated ModelPart: " + modelPart.getPartName() );
			}
		}
		return mappingProducer;
	}

	public static BasicValuedModelPart getActualGeneratedModelPart(BasicValuedModelPart modelPart) {
		// Use the root entity descriptor's identifier mapping to get the correct selection
		// expression since we always retrieve generated values for the root table only
		return modelPart.isEntityIdentifierMapping() ?
				modelPart.findContainingEntityMapping()
						.getRootEntityDescriptor()
						.getIdentifierMapping()
						.asBasicValuedModelPart() :
				modelPart;
	}

	/**
	 * Returns a list of {@link ModelPart}s that represent the actual generated values
	 * based on timing and the support flags passed in input.
	 */
	private static List<? extends ModelPart> getActualGeneratedModelParts(
			EntityPersister persister,
			EventType timing,
			boolean supportsArbitraryValues,
			boolean supportsRowId) {
		if ( timing == EventType.INSERT ) {
			final List<? extends ModelPart> generatedProperties = supportsArbitraryValues ?
					persister.getInsertGeneratedProperties() :
					List.of( persister.getIdentifierMapping() );
			if ( persister.getRowIdMapping() != null && supportsRowId ) {
				final List<ModelPart> newList = new ArrayList<>( generatedProperties.size() + 1 );
				newList.addAll( generatedProperties );
				newList.add( persister.getRowIdMapping() );
				return Collections.unmodifiableList( newList );
			}
			else {
				return generatedProperties;
			}
		}
		else {
			return persister.getUpdateGeneratedProperties();
		}
	}

	/**
	 * Creates the {@link GeneratedValuesMutationDelegate delegate} used to retrieve
	 * {@linkplain org.hibernate.generator.OnExecutionGenerator database generated values} on
	 * mutation execution through e.g. {@link Dialect#supportsInsertReturning() insert ... returning}
	 * syntax or the JDBC {@link Dialect#supportsInsertReturningGeneratedKeys() getGeneratedKeys()} API.
	 * <p>
	 * If the current {@link Dialect} doesn't support any of the available delegates this method returns {@code null}.
	 */
	public static GeneratedValuesMutationDelegate getGeneratedValuesDelegate(
			EntityPersister persister,
			EventType timing) {
		final boolean hasGeneratedProperties = !persister.getGeneratedProperties( timing ).isEmpty();
		final boolean hasRowId = timing == EventType.INSERT && persister.getRowIdMapping() != null;
		final Dialect dialect = persister.getFactory().getJdbcServices().getDialect();

		if ( hasRowId && dialect.supportsInsertReturning() && dialect.supportsInsertReturningRowId()
				&& noCustomSql( persister, timing ) ) {
			// Special case for RowId on INSERT, since GetGeneratedKeysDelegate doesn't support it
			// make InsertReturningDelegate the preferred method if the dialect supports it
			return new InsertReturningDelegate( persister, timing );
		}

		if ( !hasGeneratedProperties ) {
			return null;
		}

		if ( dialect.supportsInsertReturningGeneratedKeys()
				&& persister.getFactory().getSessionFactoryOptions().isGetGeneratedKeysEnabled() ) {
			return new GetGeneratedKeysDelegate( persister, false, timing );
		}
		else if ( supportsReturning( dialect, timing ) && noCustomSql( persister, timing ) ) {
			return new InsertReturningDelegate( persister, timing );
		}
		else if ( timing == EventType.INSERT && persister.getNaturalIdentifierProperties() != null
				&& !persister.getEntityMetamodel().isNaturalIdentifierInsertGenerated() ) {
			return new UniqueKeySelectingDelegate(
					persister,
					getNaturalIdPropertyNames( persister ),
					timing
			);
		}
		return null;
	}

	private static boolean supportsReturning(Dialect dialect, EventType timing) {
		return timing == EventType.INSERT ? dialect.supportsInsertReturning() : dialect.supportsUpdateReturning();
	}

	public static boolean noCustomSql(EntityPersister persister, EventType timing) {
		final EntityTableMapping identifierTable = persister.getIdentifierTableMapping();
		final TableMapping.MutationDetails mutationDetails = timing == EventType.INSERT ?
				identifierTable.getInsertDetails() :
				identifierTable.getUpdateDetails();
		return mutationDetails.getCustomSql() == null;
	}
}
