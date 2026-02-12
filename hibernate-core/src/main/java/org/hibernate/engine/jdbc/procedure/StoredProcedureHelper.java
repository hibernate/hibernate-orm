/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.procedure;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitStoredProcedureNameSource;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.jdbc.AbstractJdbcMutation;
import org.hibernate.sql.model.jdbc.JdbcDeleteMutation;
import org.hibernate.sql.model.jdbc.JdbcInsertMutation;
import org.hibernate.sql.model.jdbc.JdbcUpdateMutation;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.cfg.MappingSettings.IMPLICIT_NAMING_STRATEGY;
import static org.hibernate.cfg.MappingSettings.PHYSICAL_NAMING_STRATEGY;

/**
 * Rewires static {@link MutationOperation}s and {@link JdbcSelect}s to
 * call generated stored procedures and contributes the necessary stored
 * procedure DDL definitions as {@link NamedAuxiliaryDatabaseObject}s.
 *
 * @author Gavin King
 */
@Incubating
public final class StoredProcedureHelper {

	private final SessionFactoryImplementor sessionFactory;
	private final Database database;
	private final Set<String> procedureNames = new HashSet<>();
	private final Map<MutationProcedureKey, String> mutationProcedures = new HashMap<>();
	private final Map<SelectProcedureKey, String> selectProcedures = new HashMap<>();
	private final boolean enabled;


	private record MutationProcedureKey(
			String sql,
			List<String> parameterTypeNames) {
	}

	private record SelectProcedureKey(
			String sql,
			List<String> parameterTypeNames,
			List<String> resultTypeNames) {
	}

	public StoredProcedureHelper(SessionFactoryImplementor sessionFactory, Database database) {
		this.sessionFactory = sessionFactory;
		this.database = database;
		this.enabled = sessionFactory.getSessionFactoryOptions().isUseStoredProceduresEnabled();
		if ( enabled && !getDialect().getStoredProcedureSupport().supportsStoredProcedures() ) {
			throw new HibernateException(
					"Stored procedure persistence is not supported for dialect: "
					+ getDialect().getClass().getName()
			);
		}
	}

	private Dialect getDialect() {
		return database.getDialect();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public MutationOperation maybeWrapOperation(MutationOperation operation) {
		if ( enabled
				&& operation instanceof AbstractJdbcMutation jdbcMutation
				&& !jdbcMutation.isCallable() ) {
			return createCallableOperation( jdbcMutation,
					createMutationProcedure(
							mutationRoutineName( jdbcMutation ),
							jdbcMutation.getSqlString(),
							parameterTypes( jdbcMutation.getJdbcValueDescriptors() )
					) );
		}
		else {
			return operation;
		}
	}

	public JdbcSelect maybeWrapSingleIdSelect(
			JdbcSelect jdbcSelect,
			SelectStatement sqlAst,
			JdbcParametersList jdbcParameters,
			ModelPart restrictivePart,
			String rolePath) {
		if ( enabled && jdbcSelect instanceof JdbcOperationQuerySelect querySelect ) {
			return createAndWrapSelect(
					querySelect, sqlAst,
					singleIdParameterTypes( restrictivePart, jdbcParameters ),
					procedureName( rolePath )
			);
		}
		else {
			return jdbcSelect;
		}
	}

	public JdbcSelect maybeWrapBatchIdSelect(
			JdbcSelect jdbcSelect,
			SelectStatement sqlAst,
			JdbcMapping jdbcMapping,
			String rolePath) {
		if ( enabled && jdbcSelect instanceof JdbcOperationQuerySelect querySelect ) {
			return createAndWrapSelect(
					querySelect, sqlAst,
					List.of( parameterTypeName( jdbcMapping ) ),
					procedureName( rolePath )
			);
		}
		else {
			return jdbcSelect;
		}
	}

	public JdbcSelect maybeWrapNamedQuerySelect(
			JdbcSelect jdbcSelect,
			SelectStatement sqlAst,
			String namedQueryName) {
		if ( enabled && jdbcSelect instanceof JdbcOperationQuerySelect querySelect ) {
			return createAndWrapSelect(
					querySelect, sqlAst,
					queryParameterTypes( querySelect.getParameterBinders() ),
					namedQueryProcedureName( namedQueryName )
			);
		}
		return jdbcSelect;
	}

	private void contribute(String name, String createSql, String dropSql) {
		final var namespace = database.getDefaultNamespace();
		final var dialectScope = Set.of( getDialect().getClass().getName() );
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject( name, namespace, createSql, dropSql, dialectScope, false )
		);
	}

	private String createSelectProcedure(
			JdbcOperationQuerySelect querySelect,
			SelectStatement sqlAst,
			List<String> parameterTypes,
			Identifier procedureName) {
		final var sql = querySelect.getSqlString();
		final boolean requiresResultDescriptor =
				getDialect().getStoredProcedureSupport()
						.requiresSelectResultDescriptor();
		final var resultTypes =
				requiresResultDescriptor
						? queryResultTypes( sqlAst )
						: null;
		return selectProcedures.computeIfAbsent(
				new SelectProcedureKey( sql, parameterTypes, resultTypes ),
				ignored -> createSelectDefinition(
						procedureName, sql,
						parameterTypes,
						resultTypes,
						requiresResultDescriptor
								? defaultResultColumnNames( resultTypes.size() )
								: null
				) );
	}

	private JdbcSelect createAndWrapSelect(
			JdbcOperationQuerySelect querySelect,
			SelectStatement sqlAst,
			List<String> parameterTypeNames,
			Identifier procedureName) {
		return wrapQuerySelect( querySelect,
				createSelectProcedure( querySelect, sqlAst, parameterTypeNames, procedureName ) );
	}

	private List<String> parameterTypes(List<JdbcValueDescriptor> valueDescriptors) {
		final List<String> parameterTypeNames = new ArrayList<>( valueDescriptors.size() );
		for ( var valueDescriptor : valueDescriptors ) {
			parameterTypeNames.add( parameterTypeName( valueDescriptor.getJdbcMapping() ) );
		}
		return parameterTypeNames;
	}

	private List<String> singleIdParameterTypes(
			ModelPart restrictivePart,
			JdbcParametersList jdbcParameters) {
//		if ( jdbcParameters.size() == 0 ) {
//			return emptyList();
//		}
		final List<String> baseTypeNames =
				new ArrayList<>( restrictivePart.getJdbcTypeCount() );
		restrictivePart.forEachJdbcType( (index, jdbcMapping) ->
				baseTypeNames.add( parameterTypeName( jdbcMapping ) ) );
//		if ( baseTypeNames.isEmpty() ) {
//			return emptyList();
//		}
		final List<String> parameterTypeNames = new ArrayList<>( jdbcParameters.size() );
		for ( int i = 0; i < jdbcParameters.size(); i++ ) {
			parameterTypeNames.add( baseTypeNames.get( i % baseTypeNames.size() ) );
		}
		return parameterTypeNames;
	}

	private List<String> queryParameterTypes(List<JdbcParameterBinder> parameterBinders) {
		final List<String> parameterTypeNames = new ArrayList<>( parameterBinders.size() );
		for ( var parameterBinder : parameterBinders ) {
			if ( parameterBinder instanceof JdbcParameter jdbcParameter
					&& jdbcParameter.getExpressionType() != null ) {
				jdbcParameter.getExpressionType().forEachJdbcType( (index, jdbcMapping) ->
						parameterTypeNames.add( parameterTypeName( jdbcMapping ) ) );
			}
			else {
				assert false;
			}
		}
		return parameterTypeNames;
	}

	private List<String> queryResultTypes(SelectStatement sqlAst) {
		final List<String> resultTypeNames = new ArrayList<>();
		sqlAst.getQuerySpec().getSelectClause().getSqlSelections()
				.forEach( sqlSelection -> {
					final var expressionType = sqlSelection.getExpressionType();
					if ( expressionType != null ) {
						expressionType.forEachJdbcType( (index, jdbcMapping) ->
								resultTypeNames.add( resultTypeName( jdbcMapping ) ) );
					}
				} );
		return resultTypeNames;
	}

	private String parameterTypeName(JdbcMapping jdbcMapping) {
		return getDialect().getStoredProcedureSupport()
				.parameterTypeName( jdbcMapping, getDdlTypeRegistry(), getDialect() );
	}

	private String resultTypeName(JdbcMapping jdbcMapping) {
		return getDialect().getStoredProcedureSupport()
				.resultTypeName( jdbcMapping, getDdlTypeRegistry(), getDialect() );
	}

	private DdlTypeRegistry getDdlTypeRegistry() {
		return sessionFactory.getTypeConfiguration().getDdlTypeRegistry();
	}

	private String createMutationProcedure(
			Identifier procedureName,
			String sql,
			List<String> parameterTypeNames) {
		return mutationProcedures.computeIfAbsent(
				new MutationProcedureKey( sql, parameterTypeNames ),
				ignored -> {
					final String name = reserveRoutineName( procedureName );
					final var storedProcedureSupport = getDialect().getStoredProcedureSupport();
					contribute( name,
							storedProcedureSupport.createMutationProcedureDdl( name, sql, parameterTypeNames ),
							storedProcedureSupport.dropMutationProcedureDdl( name ) );
					return storedProcedureSupport.mutationInvocationSql( name, parameterTypeNames.size() );
				}
		);
	}

	private JdbcSelect wrapQuerySelect(JdbcOperationQuerySelect querySelect, String invocationSql) {
		return new JdbcOperationQuerySelect(
				invocationSql,
				querySelect.getParameterBinders(),
				querySelect.getJdbcValuesMappingProducer(),
				querySelect.getAffectedTableNames(),
				querySelect.getRowsToSkip(),
				querySelect.getMaxRows(),
				querySelect.getAppliedParameters(),
				querySelect.getLockStrategy(),
				querySelect.getOffsetParameter(),
				querySelect.getLimitParameter(),
				getDialect().getStoredProcedureSupport()
						.isSelectCallable()
		);
	}

	private static MutationOperation createCallableOperation(AbstractJdbcMutation jdbcMutation, String callSql) {
		final var parameterBinders = jdbcMutation.getParameterBinders();
		if ( jdbcMutation instanceof JdbcInsertMutation insertMutation ) {
			return new JdbcInsertMutation(
					insertMutation.getTableDetails(),
					insertMutation.getMutationTarget(),
					callSql,
					true,
					Expectation.None.INSTANCE,
					parameterBinders
			);
		}
		else if ( jdbcMutation instanceof JdbcUpdateMutation updateMutation ) {
			return new JdbcUpdateMutation(
					updateMutation.getTableDetails(),
					updateMutation.getMutationTarget(),
					callSql,
					true,
					Expectation.None.INSTANCE,
					parameterBinders
			);
		}
		else if ( jdbcMutation instanceof JdbcDeleteMutation deleteMutation ) {
			return new JdbcDeleteMutation(
					deleteMutation.getTableDetails(),
					deleteMutation.getMutationType(),
					deleteMutation.getMutationTarget(),
					callSql,
					true,
					Expectation.None.INSTANCE,
					parameterBinders
			);
		}
		else {
			return jdbcMutation;
		}
	}

	private String createSelectDefinition(
			Identifier procedureName,
			String sql,
			List<String> parameterTypeNames,
			List<String> resultTypeNames,
			List<String> resultColumnNames) {
		final var storedProcedureSupport = getDialect().getStoredProcedureSupport();
		if ( !storedProcedureSupport.supportsStoredProcedures() ) {
			throw new UnsupportedOperationException(
					"Stored procedure mutation strategy is not supported for dialect: "
							+ getDialect().getClass().getName()
			);
		}
		final String name = reserveRoutineName( procedureName );
		contribute( name,
				storedProcedureSupport.createSelectProcedureDdl(
						name, sql, parameterTypeNames, resultTypeNames, resultColumnNames ),
				storedProcedureSupport.dropSelectProcedureDdl( name, parameterTypeNames ) );
		return storedProcedureSupport.selectInvocationSql( name, parameterTypeNames.size() );
	}

	private static List<String> defaultResultColumnNames(int columnCount) {
		final List<String> resultColumnNames = new ArrayList<>( columnCount );
		for ( int i = 0; i < columnCount; i++ ) {
			resultColumnNames.add( "c" + (i + 1) );
		}
		return resultColumnNames;
	}

	private String reserveRoutineName(Identifier procedureName) {
		final String baseName = procedureName.getText();
		for ( int suffix = 1; ; suffix++ ) {
			final String candidate = suffix == 1 ? baseName : baseName + "_" + suffix;
			final String rendered =
					procedureName.isQuoted()
							? getDialect().toQuotedIdentifier( candidate )
							: candidate;
			if ( procedureNames.add( rendered ) ) {
				return rendered;
			}
		}
	}

	private Identifier mutationRoutineName(AbstractJdbcMutation jdbcMutation) {
		return Identifier.toIdentifier(
				storedProcedureName( jdbcMutation.kind(),
						jdbcMutation.getMutationTarget().getRolePath() )
		);
	}

	private Identifier procedureName(String entityName) {
		return Identifier.toIdentifier( storedProcedureName( "get", entityName ) );
	}

	private Identifier namedQueryProcedureName(String namedQueryName) {
		return Identifier.toIdentifier( storedProcedureName( "query", namedQueryName ) );
	}

	private String storedProcedureName(String prefix, String entityName) {
		final Identifier logicalIdentifier = getImplicitNamingStrategy()
				.determineStoredProcedureName( new ImplicitStoredProcedureNameSource() {
					@Override
					public String getOperation() {
						return prefix;
					}

					@Override
					public String getRolePath() {
						return entityName;
					}
				} );
		final var jdbcServices = sessionFactory.getJdbcServices();
		return getPhysicalNamingStrategy()
				.toPhysicalStoredProcedureName( logicalIdentifier, jdbcServices.getJdbcEnvironment() )
				.render( jdbcServices.getDialect() );
	}

	private ImplicitNamingStrategy getImplicitNamingStrategy() {
		final var serviceRegistry = sessionFactory.getServiceRegistry();
		return serviceRegistry.requireService( StrategySelector.class )
				.resolveDefaultableStrategy(
						ImplicitNamingStrategy.class,
						serviceRegistry.requireService( ConfigurationService.class )
								.getSettings().get( IMPLICIT_NAMING_STRATEGY ),
						ImplicitNamingStrategyJpaCompliantImpl.INSTANCE
				);
	}

	private PhysicalNamingStrategy getPhysicalNamingStrategy() {
		final var serviceRegistry = sessionFactory.getServiceRegistry();
		return serviceRegistry.requireService( StrategySelector.class )
				.resolveDefaultableStrategy(
						PhysicalNamingStrategy.class,
						serviceRegistry.requireService( ConfigurationService.class )
								.getSettings().get( PHYSICAL_NAMING_STRATEGY ),
						PhysicalNamingStrategyStandardImpl.INSTANCE
				);
	}
}
