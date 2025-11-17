/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmJsonTableFunction;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.internal.ColumnQualifierCollectorSqlAstWalker;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * HANA json_table function.
 */
public class HANAJsonTableFunction extends JsonTableFunction {

	public HANAJsonTableFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			JsonTableArguments arguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_table(" );
		arguments.jsonDocument().accept( walker );
		if ( arguments.jsonDocument() instanceof TableColumnReferenceExpression expression ) {
			sqlAppender.appendSql( ",'$' columns(" );
			for ( ColumnInfo columnInfo : expression.getIdColumns() ) {
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( ' ' );
				sqlAppender.appendSql( columnInfo.ddlType() );
				sqlAppender.appendSql( " path '$." );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( "'," );
			}

			if ( arguments.jsonPath() != null ) {
				final JsonPathPassingClause passingClause = arguments.passingClause();
				final String rawJsonPath;
				if ( passingClause != null ) {
					rawJsonPath = JsonPathHelper.inlinedJsonPathIncludingPassingClause(
							arguments.jsonPath(),
							passingClause,
							walker
					);
				}
				else {
					rawJsonPath = walker.getLiteralValue( arguments.jsonPath() );
				}
				assert rawJsonPath.charAt( 0 ) == '$';
				final String jsonPath = "$.v" + rawJsonPath.substring( 1 );

				sqlAppender.appendSql( "nested path '" );
				sqlAppender.appendSql( jsonPath );
				sqlAppender.appendSql( "' columns" );
			}
			else {
				sqlAppender.appendSql( "nested path '$.v' columns" );
			}
		}
		else if ( arguments.jsonPath() != null ) {
			sqlAppender.appendSql( ',' );
			final JsonPathPassingClause passingClause = arguments.passingClause();
			if ( passingClause != null ) {
				JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
						sqlAppender,
						"",
						arguments.jsonPath(),
						passingClause,
						walker
				);
			}
			else {
				arguments.jsonPath().accept( walker );
			}
			sqlAppender.appendSql( " columns" );
		}
		else {
			sqlAppender.appendSql( ",'$[*]' columns" );
		}
		renderColumnDefinitions( sqlAppender, arguments.columnsClause(), '(', 0, walker );
		sqlAppender.appendSql( ')' );

		if ( arguments.jsonDocument() instanceof TableColumnReferenceExpression ) {
			sqlAppender.appendSql( ')' );
		}
		// Default behavior is NULL ON ERROR
		if ( arguments.errorBehavior() == JsonTableErrorBehavior.ERROR ) {
			sqlAppender.appendSql( " error on error" );
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		final String columnType = super.determineColumnType( castTarget, walker );
		switch ( columnType ) {
			case "boolean":
				return "varchar(5)";
			case "clob":
				return "varchar(5000)";
			case "nclob":
				return "nvarchar(5000)";
		}
		return columnType;
	}

	@Override
	protected void renderColumnPath(String name, @Nullable String jsonPath, SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		if ( jsonPath != null ) {
			super.renderColumnPath( name, jsonPath, sqlAppender, walker );
		}
		else {
			// HANA requires a path
			sqlAppender.appendSql( " path '$." );
			sqlAppender.appendSql( name );
			sqlAppender.appendSql( '\'' );
		}
	}

	@Override
	protected void renderJsonExistsColumnDefinition(SqlAppender sqlAppender, JsonTableExistsColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " varchar(5) " );
		renderColumnPath( definition.name(), definition.jsonPath(), sqlAppender, walker );

		sqlAppender.appendSql( " default 'false' on empty" );
		final JsonExistsErrorBehavior errorBehavior = definition.errorBehavior();
		if ( errorBehavior == JsonExistsErrorBehavior.TRUE ) {
			sqlAppender.appendSql( " default 'true' on error" );
		}
		else if ( errorBehavior == JsonExistsErrorBehavior.ERROR ) {
			sqlAppender.appendSql( " error on error" );
		}
		else {
			sqlAppender.appendSql( " default 'false' on error" );
		}
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			QueryEngine queryEngine) {
		//noinspection unchecked
		return new SqmJsonTableFunction<>(
				this,
				this,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				(SqmExpression<?>) arguments.get( 0 ),
				arguments.size() > 1 ? (SqmExpression<String>) arguments.get( 1 ) : null
		) {
			@Override
			public TableGroup convertToSqlAst(
					NavigablePath navigablePath,
					String identifierVariable,
					boolean lateral,
					boolean canUseInnerJoins,
					boolean withOrdinality,
					SqmToSqlAstConverter walker) {
				// SAP HANA only supports table column references i.e. `TABLE_NAME.COLUMN_NAME`
				// or constants as arguments to json_table, so it's impossible to do lateral joins.
				// There is a nice trick we can apply to make this work though, which is to figure out
				// the table group an expression belongs to and render a special CTE returning xml/json that can be joined.
				// The xml/json of that CTE needs to be extended by table group primary key data,
				// so we can join it later.
				final FunctionTableGroup functionTableGroup = (FunctionTableGroup) super.convertToSqlAst(
						navigablePath,
						identifierVariable,
						lateral,
						canUseInnerJoins,
						withOrdinality,
						walker
				);
				//noinspection unchecked
				final List<SqlAstNode> sqlArguments = (List<SqlAstNode>) functionTableGroup.getPrimaryTableReference()
						.getFunctionExpression()
						.getArguments();
				final Expression argument = (Expression) sqlArguments.get( 0 );
				final Set<String> qualifiers = ColumnQualifierCollectorSqlAstWalker.determineColumnQualifiers( argument );
				// Can only do this transformation if the argument contains a single column reference qualifier
				if ( qualifiers.size() == 1 ) {
					final String tableQualifier = qualifiers.iterator().next();
					// Find the table group which the unnest argument refers to
					final FromClauseAccess fromClauseAccess = walker.getFromClauseAccess();
					final TableGroup sourceTableGroup =
							fromClauseAccess.findTableGroupByIdentificationVariable( tableQualifier );
					if ( sourceTableGroup != null ) {
						final List<ColumnInfo> idColumns = new ArrayList<>();
						addIdColumns( sourceTableGroup.getModelPart(), idColumns );

						// Register a query transformer to register the CTE and rewrite the array argument
						walker.registerQueryTransformer( (cteContainer, querySpec, converter) -> {
							// Determine a CTE name that is available
							final String baseName = "_data";
							String cteName;
							int index = 0;
							do {
								cteName = baseName + ( index++ );
							} while ( cteContainer.getCteStatement( cteName ) != null );

							final TableGroup parentTableGroup = querySpec.getFromClause().queryTableGroups(
									tg -> tg.findTableGroupJoin( functionTableGroup ) == null ? null : tg
							);
							final TableGroupJoin join = parentTableGroup.findTableGroupJoin( functionTableGroup );
							final Expression lhs = createExpression( tableQualifier, idColumns );
							final Expression rhs = createExpression(
									functionTableGroup.getPrimaryTableReference().getIdentificationVariable(),
									idColumns
							);
							join.applyPredicate( new ComparisonPredicate( lhs, ComparisonOperator.EQUAL, rhs ) );

							final String tableName = cteName;
							final List<CteColumn> cteColumns = List.of(
									new CteColumn( "v", argument.getExpressionType().getSingleJdbcMapping() )
							);
							final QuerySpec cteQuery = new QuerySpec( false );
							cteQuery.getFromClause().addRoot(
									new StandardTableGroup(
											true,
											sourceTableGroup.getNavigablePath(),
											(TableGroupProducer) sourceTableGroup.getModelPart(),
											false,
											null,
											sourceTableGroup.findTableReference( tableQualifier ),
											false,
											null,
											joinTableName -> false,
											(joinTableName, tg) -> null,
											null
									)
							);
							final Expression wrapperExpression = new JsonWrapperExpression( idColumns, tableQualifier, argument );
							cteQuery.getSelectClause().addSqlSelection( new SqlSelectionImpl( wrapperExpression ) );
							cteContainer.addCteStatement( new CteStatement(
									new CteTable( tableName, cteColumns ),
									new SelectStatement( cteQuery )
							) );
							sqlArguments.set( 0, new TableColumnReferenceExpression( argument, tableName, idColumns ) );
							return querySpec;
						} );
					}
				}
				return functionTableGroup;
			}

			private Expression createExpression(String qualifier, List<ColumnInfo> idColumns) {
				if ( idColumns.size() == 1 ) {
					final ColumnInfo columnInfo = idColumns.get( 0 );
					return new ColumnReference( qualifier, columnInfo.name(), false, null, columnInfo.jdbcMapping() );
				}
				else {
					final ArrayList<Expression> expressions = new ArrayList<>( idColumns.size() );
					for ( ColumnInfo columnInfo : idColumns ) {
						expressions.add(
								new ColumnReference(
										qualifier,
										columnInfo.name(),
										false,
										null,
										columnInfo.jdbcMapping()
								)
						);
					}
					return new SqlTuple( expressions, null );
				}
			}

			private void addIdColumns(ModelPartContainer modelPartContainer, List<ColumnInfo> idColumns) {
				if ( modelPartContainer instanceof EntityValuedModelPart entityValuedModelPart ) {
					addIdColumns( entityValuedModelPart.getEntityMappingType(), idColumns );
				}
				else if ( modelPartContainer instanceof PluralAttributeMapping pluralAttributeMapping ) {
					addIdColumns( pluralAttributeMapping, idColumns );
				}
				else if ( modelPartContainer instanceof EmbeddableValuedModelPart embeddableModelPart ) {
					addIdColumns( embeddableModelPart, idColumns );
				}
				else {
					throw new QueryException( "Unsupported model part container: " + modelPartContainer );
				}
			}

			private void addIdColumns(EmbeddableValuedModelPart embeddableModelPart, List<ColumnInfo> idColumns) {
				if ( embeddableModelPart instanceof EmbeddedCollectionPart collectionPart ) {
					addIdColumns( collectionPart.getCollectionAttribute(), idColumns );
				}
				else {
					addIdColumns( embeddableModelPart.asAttributeMapping().getDeclaringType(), idColumns );
				}
			}

			private void addIdColumns(PluralAttributeMapping pluralAttributeMapping, List<ColumnInfo> idColumns) {
				final DdlTypeRegistry ddlTypeRegistry = pluralAttributeMapping.getCollectionDescriptor()
						.getFactory()
						.getTypeConfiguration()
						.getDdlTypeRegistry();
				addIdColumns( pluralAttributeMapping.getKeyDescriptor().getKeyPart(), ddlTypeRegistry, idColumns );
			}

			private void addIdColumns(EntityMappingType entityMappingType, List<ColumnInfo> idColumns) {
				final DdlTypeRegistry ddlTypeRegistry = entityMappingType.getEntityPersister()
						.getFactory()
						.getTypeConfiguration()
						.getDdlTypeRegistry();
				addIdColumns( entityMappingType.getIdentifierMapping(), ddlTypeRegistry, idColumns );
			}

			private void addIdColumns(
					ValuedModelPart modelPart,
					DdlTypeRegistry ddlTypeRegistry,
					List<ColumnInfo> idColumns) {
				modelPart.forEachSelectable( (selectionIndex, selectableMapping) -> {
					final JdbcMapping jdbcMapping = selectableMapping.getJdbcMapping().getSingleJdbcMapping();
					idColumns.add( new ColumnInfo(
							selectableMapping.getSelectionExpression(),
							jdbcMapping,
							ddlTypeRegistry.getTypeName(
									jdbcMapping.getJdbcType().getDefaultSqlTypeCode(),
									selectableMapping.toSize(),
									(Type) jdbcMapping
							)
					) );
				} );
			}

		};
	}

	record ColumnInfo(String name, JdbcMapping jdbcMapping, String ddlType) {}

	static class TableColumnReferenceExpression implements SelfRenderingExpression {

		private final Expression argument;
		private final String tableName;
		private final List<ColumnInfo> idColumns;

		public TableColumnReferenceExpression(Expression argument, String tableName, List<ColumnInfo> idColumns) {
			this.argument = argument;
			this.tableName = tableName;
			this.idColumns = idColumns;
		}

		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> walker,
				SessionFactoryImplementor sessionFactory) {
			sqlAppender.appendSql( tableName );
			sqlAppender.appendSql( ".v" );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return argument.getExpressionType();
		}

		public List<ColumnInfo> getIdColumns() {
			return idColumns;
		}
	}

	static class JsonWrapperExpression implements SelfRenderingExpression {
		private final List<ColumnInfo> idColumns;
		private final String tableQualifier;
		private final Expression argument;

		public JsonWrapperExpression(List<ColumnInfo> idColumns, String tableQualifier, Expression argument) {
			this.idColumns = idColumns;
			this.tableQualifier = tableQualifier;
			this.argument = argument;
		}

		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> walker,
				SessionFactoryImplementor sessionFactory) {
			// Produce a JSON string e.g. {"id":1,"v":[...]}
			// which will contain the original JSON as well as id column information for correlation
			sqlAppender.appendSql( "'{'||trim('{}' from (select" );
			char separator = ' ';
			for ( ColumnInfo columnInfo : idColumns ) {
				sqlAppender.appendSql( separator );
				sqlAppender.appendSql( tableQualifier );
				sqlAppender.appendSql( '.' );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( ' ' );
				sqlAppender.appendDoubleQuoteEscapedString( columnInfo.name() );
				separator = ',';
			}
			sqlAppender.appendSql( " from sys.dummy for json('arraywrap'='no')))||" );
			sqlAppender.appendSql( "',\"v\":'||" );
			argument.accept( walker );
			sqlAppender.appendSql( "||'}'" );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return argument.getExpressionType();
		}
	}
}
