/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.QueryException;
import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.dialect.function.json.ExpressionTypeHelper;
import org.hibernate.dialect.function.json.HANAJsonValueFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.internal.ColumnQualifierCollectorSqlAstWalker;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
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
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * HANA unnest function.
 */
public class HANAUnnestFunction extends UnnestFunction {

	public HANAUnnestFunction() {
		super( "v", "i" );
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			QueryEngine queryEngine) {
		return new SelfRenderingSqmSetReturningFunction<>(
				this,
				this,
				arguments,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
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
				// or constants as arguments to xmltable/json_table, so it's impossible to do lateral joins.
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
							final Expression wrapperExpression;
							if ( ExpressionTypeHelper.isXml( argument ) ) {
								wrapperExpression = new XmlWrapperExpression( idColumns, tableQualifier, argument );
								// xmltable is allergic to null values and produces no result if one occurs,
								// so we must filter them out
								cteQuery.applyPredicate( new NullnessPredicate( argument, true ) );
							}
							else {
								wrapperExpression = new JsonWrapperExpression( idColumns, tableQualifier, argument );
							}
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

	@Override
	protected void renderXmlTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final XmlHelper.CollectionTags collectionTags = XmlHelper.determineCollectionTags(
				(BasicPluralJavaType<?>) pluralType.getJavaTypeDescriptor(), walker.getSessionFactory()
		);

		sqlAppender.appendSql( "xmltable('/" );
		sqlAppender.appendSql( collectionTags.rootName() );
		sqlAppender.appendSql( '/' );
		sqlAppender.appendSql( collectionTags.elementName() );
		sqlAppender.appendSql( "' passing " );
		array.accept( walker );
		sqlAppender.appendSql( " columns" );
		char separator = ' ';
		final int offset;
		if ( array instanceof TableColumnReferenceExpression expression ) {
			offset = expression.getIdColumns().size();
			for ( ColumnInfo columnInfo : expression.getIdColumns() ) {
				sqlAppender.appendSql( separator );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( ' ' );
				sqlAppender.appendSql( columnInfo.ddlType() );
				sqlAppender.appendSql( " path 'ancestor::" );
				sqlAppender.appendSql( collectionTags.rootName() );
				sqlAppender.appendSql( "/@" );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( '\'' );
				separator = ',';
			}
		}
		else {
			offset = 0;
		}
		if ( tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null ) == null ) {
			tupleType.forEachSelectable( offset, (selectionIndex, selectableMapping) -> {
				if ( selectionIndex == 0 ) {
					sqlAppender.append( ' ' );
				}
				else {
					sqlAppender.append( ',' );
				}
				sqlAppender.append( selectableMapping.getSelectionExpression() );
				if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.append( " for ordinality" );
				}
				else {
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( " path '" );
					sqlAppender.appendSql( selectableMapping.getSelectableName() );
					sqlAppender.appendSql( "'" );
				}
			} );
		}
		else {
			tupleType.forEachSelectable( offset, (selectionIndex, selectableMapping) -> {
				if ( selectionIndex == 0 ) {
					sqlAppender.append( ' ' );
				}
				else {
					sqlAppender.append( ',' );
				}
				sqlAppender.append( selectableMapping.getSelectionExpression() );
				if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.append( " for ordinality" );
				}
				else {
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( " path '" );
					sqlAppender.appendSql( "." );
					sqlAppender.appendSql( "'" );
				}
			} );
		}

		sqlAppender.appendSql( ')' );
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
			final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) argument.getExpressionType().getSingleJdbcMapping();
			final XmlHelper.CollectionTags collectionTags = XmlHelper.determineCollectionTags(
					(BasicPluralJavaType<?>) pluralType.getJavaTypeDescriptor(),
					sessionFactory
			);

			// Produce an XML string e.g. <root id="1">...</root>
			// which will contain the original XML as well as id column information for correlation
			sqlAppender.appendSql( "trim('/>' from (select" );
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
			sqlAppender.appendSql( " from sys.dummy for xml('root'='no','columnstyle'='attribute','rowname'='" );
			sqlAppender.appendSql( collectionTags.rootName() );
			sqlAppender.appendSql( "','format'='no')))||substring(" );
			argument.accept( walker );
			sqlAppender.appendSql( ",locate('<" );
			sqlAppender.appendSql( collectionTags.rootName() );
			sqlAppender.appendSql( ">'," );
			argument.accept( walker );
			sqlAppender.appendSql( ")+" );
			sqlAppender.appendSql( collectionTags.rootName().length() + 2 );
			sqlAppender.appendSql( ",length(" );
			argument.accept( walker );
			sqlAppender.appendSql( "))" );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return argument.getExpressionType();
		}
	}

	@Override
	protected String getDdlType(SqlTypedMapping sqlTypedMapping, int containerSqlTypeCode, SqlAstTranslator<?> translator) {
		final String ddlType = super.getDdlType( sqlTypedMapping, containerSqlTypeCode, translator );
		if ( containerSqlTypeCode == SqlTypes.JSON_ARRAY ) {
			return HANAJsonValueFunction.jsonValueReturningType( ddlType );
		}
		return ddlType;
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_table(" );
		array.accept( walker );

		if ( array instanceof TableColumnReferenceExpression expression ) {
			sqlAppender.appendSql( ",'$' columns(" );
			for ( ColumnInfo columnInfo : expression.getIdColumns() ) {
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( ' ' );
				sqlAppender.appendSql( columnInfo.ddlType() );
				sqlAppender.appendSql( " path '$." );
				sqlAppender.appendSql( columnInfo.name() );
				sqlAppender.appendSql( "'," );
			}

			sqlAppender.appendSql( "nested path '$.v' columns" );
			renderJsonTableColumns( sqlAppender, tupleType, walker, true );
			sqlAppender.appendSql( "))" );
		}
		else {
			sqlAppender.appendSql( ",'$[*]' columns" );
			renderJsonTableColumns( sqlAppender, tupleType, walker, true );
			sqlAppender.appendSql( ")" );
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
			sqlAppender.appendSql( "',\"v\":'||case when " );
			argument.accept( walker );
			sqlAppender.appendSql( " not like '[]' then " );
			argument.accept( walker );
			sqlAppender.appendSql( " end||'}'" );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return argument.getExpressionType();
		}
	}
}
