/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.derived.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmXmlTableFunction;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.Template;
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
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.XmlTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hibernate.sql.ast.spi.AbstractSqlAstTranslator.isParameter;

/**
 * HANA xmltable function.
 */
public class HANAXmlTableFunction extends XmlTableFunction {

	public HANAXmlTableFunction(TypeConfiguration typeConfiguration) {
		super( false, new DB2XmlTableSetReturningFunctionTypeResolver(), typeConfiguration );
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			QueryEngine queryEngine) {
		//noinspection unchecked
		return new SqmXmlTableFunction<>(
				this,
				this,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				(SqmExpression<String>) arguments.get( 0 ),
				(SqmExpression<?>) arguments.get( 1 )
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
				// or constants as arguments to xmltable, so it's impossible to do lateral joins.
				// There is a nice trick we can apply to make this work though, which is to figure out
				// the table group an expression belongs to and render a special CTE returning xml/json that can be joined.
				// The xml of that CTE needs to be extended by table group primary key data,
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
				final Expression document = (Expression) sqlArguments.get( 1 );
				final Set<String> qualifiers = ColumnQualifierCollectorSqlAstWalker.determineColumnQualifiers( document );
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
									new CteColumn( "v", document.getExpressionType().getSingleJdbcMapping() )
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
							final Expression wrapperExpression = new XmlWrapperExpression( idColumns, tableQualifier, document );
							// xmltable is allergic to null values and produces no result if one occurs,
							// so we must filter them out
							cteQuery.applyPredicate( new NullnessPredicate( document, true ) );
							cteQuery.getSelectClause().addSqlSelection( new SqlSelectionImpl( wrapperExpression ) );
							cteContainer.addCteStatement( new CteStatement(
									new CteTable( tableName, cteColumns ),
									new SelectStatement( cteQuery )
							) );
							sqlArguments.set( 1, new TableColumnReferenceExpression( document, tableName, idColumns ) );
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

	static class XmlWrapperExpression implements SelfRenderingExpression {
		private final List<ColumnInfo> idColumns;
		private final String tableQualifier;
		private final Expression argument;

		public XmlWrapperExpression(List<ColumnInfo> idColumns, String tableQualifier, Expression argument) {
			this.idColumns = idColumns;
			this.tableQualifier = tableQualifier;
			this.argument = argument;
		}

		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> walker,
				SessionFactoryImplementor sessionFactory) {
			// Produce an XML string e.g. <root id="1">...</root>
			// which will contain the original XML as well as id column information for correlation
			sqlAppender.appendSql( "'<root'" );
			for ( ColumnInfo columnInfo : idColumns ) {
				sqlAppender.appendSql( "||' " );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( "=\"'||" );
				sqlAppender.appendSql( tableQualifier );
				sqlAppender.appendSql( '.' );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( "||'\"'" );
			}
			sqlAppender.appendSql( "||'>'||" );
			argument.accept( walker );
			sqlAppender.appendSql( "||'</root>'" );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return argument.getExpressionType();
		}
	}

	@Override
	protected void renderXmlTable(SqlAppender sqlAppender, XmlTableArguments arguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmltable(" );
		final Expression documentExpression = arguments.xmlDocument();
		final String xpath = walker.getLiteralValue( arguments.xpath() );
		if ( documentExpression instanceof TableColumnReferenceExpression ) {
			sqlAppender.appendSingleQuoteEscapedString( "/root" + xpath );
		}
		else {
			sqlAppender.appendSingleQuoteEscapedString( xpath );
		}
		sqlAppender.appendSql( " passing " );
		// We have to handle the rendering of strings/literals manually here to avoid using nationalized literals,
		// because HANA doesn't support that syntax in xmltable()
		if ( documentExpression instanceof Literal literal ) {
			sqlAppender.appendSingleQuoteEscapedString( (String) literal.getLiteralValue() );
		}
		else if ( isParameter( documentExpression ) ) {
			sqlAppender.appendSingleQuoteEscapedString( walker.getLiteralValue( documentExpression ) );
		}
		else {
			documentExpression.accept( walker );
		}
		renderColumns( sqlAppender, arguments.columnsClause(), walker );
		if ( documentExpression instanceof TableColumnReferenceExpression expression ) {
			for ( ColumnInfo columnInfo : expression.getIdColumns() ) {
				sqlAppender.appendSql( ',' );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( ' ' );
				sqlAppender.appendSql( columnInfo.ddlType() );
				sqlAppender.appendSql( " path '/root/@" );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( '\'' );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		final String typeName = super.determineColumnType( castTarget, walker );
		return switch ( typeName ) {
			// xmltable doesn't support tinyint. Usually it is a boolean, but if not, use "integer"
			case "tinyint" -> isBoolean( castTarget.getJdbcMapping() ) ? "varchar(5)" : "integer";
			// Also, smallint isn't supported
			case "smallint" -> "integer";
			// For boolean, use varchar since that decoding is done through a read expression
			case "boolean" -> "varchar(5)";
			// Float is also not supported, but double is
			case "float" -> "double";
			// Clobs are also not supported, so use the biggest nvarchar possible
			case "clob", "nclob" -> "nvarchar(5000)";
			default -> typeName;
		};
	}

	@Override
	protected void renderXmlQueryColumnDefinition(SqlAppender sqlAppender, XmlTableQueryColumnDefinition definition, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( new CastTarget( definition.type() ), walker ) );
		sqlAppender.appendSql( " format xml" );

		renderColumnPath( definition.name(), definition.xpath(), sqlAppender, walker );
		renderDefaultExpression( definition.defaultExpression(), sqlAppender, walker );
	}

	static boolean isBoolean(JdbcMapping type) {
		return type.getJdbcType().isBoolean();
	}

	private static class DB2XmlTableSetReturningFunctionTypeResolver extends XmlTableSetReturningFunctionTypeResolver {
		@Override
		protected void addSelectableMapping(List<SelectableMapping> selectableMappings, String name, JdbcMapping type, SqmToSqlAstConverter converter) {
			if ( isBoolean( type ) ) {
				//noinspection unchecked
				final JdbcLiteralFormatter<Object> jdbcLiteralFormatter = type.getJdbcLiteralFormatter();
				final SessionFactoryImplementor sessionFactory = converter.getCreationContext().getSessionFactory();
				final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
				final WrapperOptions wrapperOptions = sessionFactory.getWrapperOptions();
				final Object trueValue = type.convertToRelationalValue( true );
				final Object falseValue = type.convertToRelationalValue( false );
				final String trueFragment = jdbcLiteralFormatter.toJdbcLiteral( trueValue, dialect, wrapperOptions );
				final String falseFragment = jdbcLiteralFormatter.toJdbcLiteral( falseValue, dialect, wrapperOptions );
				selectableMappings.add( new SelectableMappingImpl(
						"",
						name,
						new SelectablePath( name ),
						"case " + Template.TEMPLATE + "." + name + " when 'true' then " + trueFragment + " when 'false' then " + falseFragment + " end",
						null,
						"varchar(5)",
						null,
						null,
						null,
						null,
						false,
						false,
						false,
						false,
						false,
						false,
						type
				));
			}
			else {
				super.addSelectableMapping( selectableMappings, name, type, converter );
			}
		}
	}
}
