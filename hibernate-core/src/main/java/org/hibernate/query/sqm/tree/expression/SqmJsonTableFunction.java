/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.query.criteria.JpaCastTarget;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJsonExistsNode;
import org.hibernate.query.criteria.JpaJsonQueryNode;
import org.hibernate.query.criteria.JpaJsonTableColumnsNode;
import org.hibernate.query.criteria.JpaJsonTableFunction;
import org.hibernate.query.criteria.JpaJsonValueNode;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.function.SetReturningFunctionRenderer;
import org.hibernate.query.sqm.function.SqmSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableNestedColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableValueColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @since 7.0
 */
public class SqmJsonTableFunction<T> extends SelfRenderingSqmSetReturningFunction<T> implements JpaJsonTableFunction {

	private final Set<String> columnNames = new HashSet<>();
	private final Columns columns;
	private @Nullable Map<String, SqmExpression<?>> passingExpressions;
	private ErrorBehavior errorBehavior;

	public SqmJsonTableFunction(
			SqmSetReturningFunctionDescriptor descriptor,
			SetReturningFunctionRenderer renderer,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver setReturningTypeResolver,
			NodeBuilder nodeBuilder,
			SqmExpression<?> document,
			@Nullable SqmExpression<String> jsonPath) {
		this(
				descriptor,
				renderer,
				jsonPath == null ? Arrays.asList( document, null ) : Arrays.asList( document, jsonPath, null ),
				argumentsValidator,
				setReturningTypeResolver,
				nodeBuilder,
				null,
				ErrorBehavior.UNSPECIFIED
		);
	}

	private SqmJsonTableFunction(
			SqmSetReturningFunctionDescriptor descriptor,
			SetReturningFunctionRenderer renderer,
			List<SqmTypedNode<?>> arguments,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver setReturningTypeResolver,
			NodeBuilder nodeBuilder,
			@Nullable Map<String, SqmExpression<?>> passingExpressions,
			ErrorBehavior errorBehavior) {
		super( descriptor, renderer, arguments, argumentsValidator, setReturningTypeResolver, nodeBuilder, "json_table" );
		this.columns = new Columns( this );
		this.passingExpressions = passingExpressions;
		this.errorBehavior = errorBehavior;
		arguments.set( arguments.size() - 1, this.columns );
	}

	public Map<String, SqmExpression<?>> getPassingExpressions() {
		return passingExpressions == null ? Collections.emptyMap() : Collections.unmodifiableMap( passingExpressions );
	}

	@Override
	public JpaJsonTableFunction passing(String parameterName, Expression<?> expression) {
		if ( columns.jsonPath == null ) {
			throw new IllegalStateException( "Can't pass parameter '" + parameterName + "', because json_table has no JSON path" );
		}
		if ( passingExpressions == null ) {
			passingExpressions = new HashMap<>();
		}
		passingExpressions.put( parameterName, (SqmExpression<?>) expression );
		return this;
	}

	@Override
	public SqmJsonTableFunction<T> copy(SqmCopyContext context) {
		final SqmJsonTableFunction<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<? extends SqmTypedNode<?>> arguments = getArguments();
		final List<SqmTypedNode<?>> argumentsCopy = new ArrayList<>( arguments.size() );
		for ( int i = 0; i < arguments.size() - 1; i++ ) {
			argumentsCopy.add( arguments.get( i ).copy( context ) );
		}
		final Map<String, SqmExpression<?>> passingExpressions;
		if ( this.passingExpressions == null ) {
			passingExpressions = null;
		}
		else {
			passingExpressions = new HashMap<>( this.passingExpressions.size() );
			for ( Map.Entry<String, SqmExpression<?>> entry : this.passingExpressions.entrySet() ) {
				passingExpressions.put( entry.getKey(), entry.getValue().copy( context ) );
			}
		}
		final SqmJsonTableFunction<T> tableFunction = new SqmJsonTableFunction<>(
				getFunctionDescriptor(),
				getFunctionRenderer(),
				argumentsCopy,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				nodeBuilder(),
				passingExpressions,
				errorBehavior
		);
		context.registerCopy( this, tableFunction );
		tableFunction.columnNames.addAll( columnNames );
		tableFunction.columns.columnDefinitions.ensureCapacity( columns.columnDefinitions.size() );
		for ( ColumnDefinition columnDefinition : columns.columnDefinitions ) {
			tableFunction.columns.columnDefinitions.add( columnDefinition.copy( context ) );
		}
		return tableFunction;
	}

	@Override
	protected List<SqlAstNode> resolveSqlAstArguments(List<? extends SqmTypedNode<?>> sqmArguments, SqmToSqlAstConverter walker) {
		final List<SqlAstNode> sqlAstNodes = super.resolveSqlAstArguments( sqmArguments, walker );
		// The last argument is the SqmJsonTableFunction.Columns which will convert to null, so remove that
		sqlAstNodes.remove( sqlAstNodes.size() - 1 );

		final JsonPathPassingClause jsonPathPassingClause = createJsonPathPassingClause( walker );
		if ( jsonPathPassingClause != null ) {
			sqlAstNodes.add( jsonPathPassingClause );
		}
		switch ( errorBehavior ) {
			case NULL -> sqlAstNodes.add( JsonTableErrorBehavior.NULL );
			case ERROR -> sqlAstNodes.add( JsonTableErrorBehavior.ERROR );
			case UNSPECIFIED -> {
			}
		}
		final List<JsonTableColumnDefinition> definitions = new ArrayList<>( columns.columnDefinitions.size() );
		for ( ColumnDefinition columnDefinition : columns.columnDefinitions ) {
			definitions.add( columnDefinition.convertToSqlAst( walker ) );
		}
		sqlAstNodes.add( new JsonTableColumnsClause( definitions ) );
		return sqlAstNodes;
	}

	protected @Nullable JsonPathPassingClause createJsonPathPassingClause(SqmToSqlAstConverter walker) {
		if ( passingExpressions == null || passingExpressions.isEmpty() ) {
			return null;
		}
		final HashMap<String, org.hibernate.sql.ast.tree.expression.Expression> converted = new HashMap<>( passingExpressions.size() );
		for ( Map.Entry<String, SqmExpression<?>> entry : passingExpressions.entrySet() ) {
			converted.put( entry.getKey(), (org.hibernate.sql.ast.tree.expression.Expression) entry.getValue().accept( walker ) );
		}
		return new JsonPathPassingClause( converted );
	}

	@Override
	public ErrorBehavior getErrorBehavior() {
		return errorBehavior;
	}

	@Override
	public JpaJsonTableFunction unspecifiedOnError() {
		checkTypeResolved();
		this.errorBehavior = ErrorBehavior.UNSPECIFIED;
		return this;
	}

	@Override
	public SqmJsonTableFunction<T> nullOnError() {
		checkTypeResolved();
		this.errorBehavior = ErrorBehavior.NULL;
		return this;
	}

	@Override
	public SqmJsonTableFunction<T> errorOnError() {
		checkTypeResolved();
		this.errorBehavior = ErrorBehavior.ERROR;
		return this;
	}

	@Override
	public JpaJsonExistsNode existsColumn(String columnName) {
		return existsColumn( columnName, null );
	}

	@Override
	public JpaJsonExistsNode existsColumn(String columnName, @Nullable String jsonPath) {
		return columns.existsColumn( columnName, jsonPath );
	}

	@Override
	public JpaJsonQueryNode queryColumn(String columnName) {
		return queryColumn( columnName, null );
	}

	@Override
	public JpaJsonQueryNode queryColumn(String columnName, @Nullable String jsonPath) {
		return columns.queryColumn( columnName, jsonPath );
	}

	@Override
	public <X> JpaJsonValueNode<X> valueColumn(String columnName, Class<X> type) {
		return valueColumn( columnName, type, null );
	}

	@Override
	public <X> JpaJsonValueNode<X> valueColumn(String columnName, Class<X> type, String jsonPath) {
		return columns.valueColumn( columnName, type, jsonPath );
	}

	@Override
	public <X> JpaJsonValueNode<X> valueColumn(String columnName, JpaCastTarget<X> type, String jsonPath) {
		return columns.valueColumn( columnName, type, jsonPath );
	}

	@Override
	public <X> JpaJsonValueNode<X> valueColumn(String columnName, JpaCastTarget<X> type) {
		return columns.valueColumn( columnName, type );
	}

	@Override
	public JpaJsonTableColumnsNode nested(String jsonPath) {
		return columns.nested( jsonPath );
	}

	@Override
	public JpaJsonTableFunction ordinalityColumn(String columnName) {
		columns.ordinalityColumn( columnName );
		return this;
	}

	private void addColumn(String columnName) {
		checkTypeResolved();
		if ( !columnNames.add( columnName ) ) {
			throw new IllegalStateException( "Duplicate column: " + columnName );
		}
	}

	private void checkTypeResolved() {
		if ( isTypeResolved() ) {
			throw new IllegalStateException(
					"Type for json_table function is already resolved. Mutation is not allowed anymore" );
		}
	}

	sealed interface ColumnDefinition {
		ColumnDefinition copy(SqmCopyContext context);

		JsonTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker);

		void appendHqlString(StringBuilder sb, SqmRenderContext context);

		int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes);
	}

	static final class ExistsColumnDefinition implements ColumnDefinition, JpaJsonExistsNode {
		private final String name;
		private final BasicType<Boolean> type;
		private final @Nullable String jsonPath;
		private JpaJsonExistsNode.ErrorBehavior errorBehavior = ErrorBehavior.UNSPECIFIED;

		ExistsColumnDefinition(String name, BasicType<Boolean> type, @Nullable String jsonPath) {
			this.name = name;
			this.type = type;
			this.jsonPath = jsonPath;
		}

		private ExistsColumnDefinition(String name, BasicType<Boolean> type, @Nullable String jsonPath, ErrorBehavior errorBehavior) {
			this.name = name;
			this.type = type;
			this.jsonPath = jsonPath;
			this.errorBehavior = errorBehavior;
		}

		@Override
		public ColumnDefinition copy(SqmCopyContext context) {
			return new ExistsColumnDefinition( name, type, jsonPath, errorBehavior );
		}

		@Override
		public JsonTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker) {
			return new JsonTableExistsColumnDefinition(
					name,
					type,
					jsonPath,
					switch ( errorBehavior ) {
						case ERROR -> JsonExistsErrorBehavior.ERROR;
						case FALSE -> JsonExistsErrorBehavior.FALSE;
						case TRUE -> JsonExistsErrorBehavior.TRUE;
						case UNSPECIFIED -> null;
					}
			);
		}

		@Override
		public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
			sb.append( name );
			sb.append( " exists" );

			if ( jsonPath != null ) {
				sb.append( " path " );
				QuotingHelper.appendSingleQuoteEscapedString( sb, jsonPath );
			}

			switch ( errorBehavior ) {
				case ERROR -> sb.append( " error on error" );
				case TRUE -> sb.append( " true on error" );
				case FALSE -> sb.append( " false on error" );
			}
			sb.append( ')' );
		}

		@Override
		public int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes) {
			componentNames[offset] = name;
			componentTypes[offset] = type;
			return 1;
		}

		@Override
		public JpaJsonExistsNode.ErrorBehavior getErrorBehavior() {
			return errorBehavior;
		}

		@Override
		public JpaJsonExistsNode unspecifiedOnError() {
			errorBehavior = ErrorBehavior.UNSPECIFIED;
			return this;
		}

		@Override
		public JpaJsonExistsNode errorOnError() {
			errorBehavior = ErrorBehavior.ERROR;
			return this;
		}

		@Override
		public JpaJsonExistsNode trueOnError() {
			errorBehavior = ErrorBehavior.TRUE;
			return this;
		}

		@Override
		public JpaJsonExistsNode falseOnError() {
			errorBehavior = ErrorBehavior.FALSE;
			return this;
		}
	}

	static final class QueryColumnDefinition implements ColumnDefinition, JpaJsonQueryNode {
		private final String name;
		private final BasicType<String> type;
		private final @Nullable String jsonPath;
		private JpaJsonQueryNode.WrapMode wrapMode = WrapMode.UNSPECIFIED;
		private JpaJsonQueryNode.ErrorBehavior errorBehavior = ErrorBehavior.UNSPECIFIED;
		private JpaJsonQueryNode.EmptyBehavior emptyBehavior = EmptyBehavior.UNSPECIFIED;

		QueryColumnDefinition(String name, BasicType<String> type, @Nullable String jsonPath) {
			this.name = name;
			this.type = type;
			this.jsonPath = jsonPath;
		}

		private QueryColumnDefinition(String name, BasicType<String> type, @Nullable String jsonPath, WrapMode wrapMode, ErrorBehavior errorBehavior, EmptyBehavior emptyBehavior) {
			this.name = name;
			this.type = type;
			this.jsonPath = jsonPath;
			this.wrapMode = wrapMode;
			this.errorBehavior = errorBehavior;
			this.emptyBehavior = emptyBehavior;
		}

		@Override
		public ColumnDefinition copy(SqmCopyContext context) {
			return new QueryColumnDefinition( name, type, jsonPath, wrapMode, errorBehavior, emptyBehavior );
		}

		@Override
		public JsonTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker) {
			return new JsonTableQueryColumnDefinition(
					name,
					type,
					jsonPath,
					switch ( wrapMode ) {
						case WITH_WRAPPER -> JsonQueryWrapMode.WITH_WRAPPER;
						case WITHOUT_WRAPPER -> JsonQueryWrapMode.WITHOUT_WRAPPER;
						case WITH_CONDITIONAL_WRAPPER -> JsonQueryWrapMode.WITH_CONDITIONAL_WRAPPER;
						case UNSPECIFIED -> null;
					},
					switch ( errorBehavior ) {
						case ERROR -> JsonQueryErrorBehavior.ERROR;
						case NULL -> JsonQueryErrorBehavior.NULL;
						case EMPTY_ARRAY -> JsonQueryErrorBehavior.EMPTY_ARRAY;
						case EMPTY_OBJECT -> JsonQueryErrorBehavior.EMPTY_OBJECT;
						case UNSPECIFIED -> null;
					},
					switch ( emptyBehavior ) {
						case ERROR -> JsonQueryEmptyBehavior.ERROR;
						case NULL -> JsonQueryEmptyBehavior.NULL;
						case EMPTY_ARRAY -> JsonQueryEmptyBehavior.EMPTY_ARRAY;
						case EMPTY_OBJECT -> JsonQueryEmptyBehavior.EMPTY_OBJECT;
						case UNSPECIFIED -> null;
					}
			);
		}

		@Override
		public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
			sb.append( name );
			sb.append( " json" );
			switch ( wrapMode ) {
				case WITH_WRAPPER -> sb.append( " with wrapper" );
				case WITHOUT_WRAPPER -> sb.append( " without wrapper" );
				case WITH_CONDITIONAL_WRAPPER -> sb.append( " with conditional wrapper" );
			}
			if ( jsonPath != null ) {
				sb.append( " path " );
				QuotingHelper.appendSingleQuoteEscapedString( sb, jsonPath );
			}
			switch ( errorBehavior ) {
				case NULL -> sb.append( " null on error" );
				case ERROR -> sb.append( " error on error" );
				case EMPTY_ARRAY -> sb.append( " empty array on error" );
				case EMPTY_OBJECT -> sb.append( " empty object on error" );
			}
			switch ( emptyBehavior ) {
				case NULL -> sb.append( " null on empty" );
				case ERROR -> sb.append( " error on empty" );
				case EMPTY_ARRAY -> sb.append( " empty array on empty" );
				case EMPTY_OBJECT -> sb.append( " empty object on empty" );
			}
		}

		@Override
		public int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes) {
			componentNames[offset] = name;
			componentTypes[offset] = type;
			return 1;
		}

		@Override
		public WrapMode getWrapMode() {
			return wrapMode;
		}

		@Override
		public ErrorBehavior getErrorBehavior() {
			return errorBehavior;
		}

		@Override
		public EmptyBehavior getEmptyBehavior() {
			return emptyBehavior;
		}

		@Override
		public JpaJsonQueryNode withoutWrapper() {
			wrapMode = WrapMode.WITHOUT_WRAPPER;
			return this;
		}

		@Override
		public JpaJsonQueryNode withWrapper() {
			wrapMode = WrapMode.WITH_WRAPPER;
			return this;
		}

		@Override
		public JpaJsonQueryNode withConditionalWrapper() {
			wrapMode = WrapMode.WITH_CONDITIONAL_WRAPPER;
			return this;
		}

		@Override
		public JpaJsonQueryNode unspecifiedWrapper() {
			wrapMode = WrapMode.UNSPECIFIED;
			return this;
		}

		@Override
		public JpaJsonQueryNode unspecifiedOnError() {
			errorBehavior = ErrorBehavior.UNSPECIFIED;
			return this;
		}

		@Override
		public JpaJsonQueryNode errorOnError() {
			errorBehavior = ErrorBehavior.ERROR;
			return this;
		}

		@Override
		public JpaJsonQueryNode nullOnError() {
			errorBehavior = ErrorBehavior.NULL;
			return this;
		}

		@Override
		public JpaJsonQueryNode emptyArrayOnError() {
			errorBehavior = ErrorBehavior.EMPTY_ARRAY;
			return this;
		}

		@Override
		public JpaJsonQueryNode emptyObjectOnError() {
			errorBehavior = ErrorBehavior.EMPTY_OBJECT;
			return this;
		}

		@Override
		public JpaJsonQueryNode unspecifiedOnEmpty() {
			emptyBehavior = EmptyBehavior.UNSPECIFIED;
			return this;
		}

		@Override
		public JpaJsonQueryNode errorOnEmpty() {
			emptyBehavior = EmptyBehavior.ERROR;
			return this;
		}

		@Override
		public JpaJsonQueryNode nullOnEmpty() {
			emptyBehavior = EmptyBehavior.NULL;
			return this;
		}

		@Override
		public JpaJsonQueryNode emptyArrayOnEmpty() {
			emptyBehavior = EmptyBehavior.EMPTY_ARRAY;
			return this;
		}

		@Override
		public JpaJsonQueryNode emptyObjectOnEmpty() {
			emptyBehavior = EmptyBehavior.EMPTY_OBJECT;
			return this;
		}
	}

	static final class ValueColumnDefinition<X> implements ColumnDefinition, JpaJsonValueNode<X> {
		private final String name;
		private final SqmCastTarget<?> type;
		private final @Nullable String jsonPath;
		private JpaJsonValueNode.ErrorBehavior errorBehavior = ErrorBehavior.UNSPECIFIED;
		private JpaJsonValueNode.EmptyBehavior emptyBehavior = EmptyBehavior.UNSPECIFIED;
		private @Nullable SqmExpression<X> errorDefaultExpression;
		private @Nullable SqmExpression<X> emptyDefaultExpression;

		ValueColumnDefinition(String name, SqmCastTarget<?> type, @Nullable String jsonPath) {
			this.name = name;
			this.type = type;
			this.jsonPath = jsonPath;
		}

		private ValueColumnDefinition(
				String name,
				SqmCastTarget<?> type,
				@Nullable String jsonPath,
				ErrorBehavior errorBehavior,
				EmptyBehavior emptyBehavior,
				@Nullable SqmExpression<X> errorDefaultExpression,
				@Nullable SqmExpression<X> emptyDefaultExpression) {
			this.name = name;
			this.type = type;
			this.jsonPath = jsonPath;
			this.errorBehavior = errorBehavior;
			this.emptyBehavior = emptyBehavior;
			this.errorDefaultExpression = errorDefaultExpression;
			this.emptyDefaultExpression = emptyDefaultExpression;
		}

		@Override
		public ColumnDefinition copy(SqmCopyContext context) {
			return new ValueColumnDefinition<>(
					name,
					type,
					jsonPath,
					errorBehavior,
					emptyBehavior,
					errorDefaultExpression == null ? null : errorDefaultExpression.copy( context ),
					emptyDefaultExpression == null ? null : emptyDefaultExpression.copy( context )
			);
		}

		@Override
		public JsonTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker) {
			return new JsonTableValueColumnDefinition(
					name,
					(org.hibernate.sql.ast.tree.expression.CastTarget) type.accept( walker ),
					jsonPath,
					switch ( errorBehavior ) {
						case UNSPECIFIED -> null;
						case NULL -> JsonValueErrorBehavior.NULL;
						case ERROR -> JsonValueErrorBehavior.ERROR;
						case DEFAULT -> JsonValueErrorBehavior.defaultOnError(
								(org.hibernate.sql.ast.tree.expression.Expression)
										castNonNull( errorDefaultExpression ).accept( walker )
						);
					},
					switch ( emptyBehavior ) {
						case UNSPECIFIED -> null;
						case NULL -> JsonValueEmptyBehavior.NULL;
						case ERROR -> JsonValueEmptyBehavior.ERROR;
						case DEFAULT -> JsonValueEmptyBehavior.defaultOnEmpty(
								(org.hibernate.sql.ast.tree.expression.Expression)
										castNonNull( emptyDefaultExpression ).accept( walker )
						);
					}
			);
		}

		@Override
		public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
			sb.append( name );
			sb.append( ' ' );
			type.appendHqlString( sb, context );
			if ( jsonPath != null ) {
				sb.append( " path " );
				QuotingHelper.appendSingleQuoteEscapedString( sb, jsonPath );
			}
			switch ( errorBehavior ) {
				case NULL -> sb.append( " null on error" );
				case ERROR -> sb.append( " error on error" );
				case DEFAULT -> {
					assert errorDefaultExpression != null;
					sb.append( " default " );
					errorDefaultExpression.appendHqlString( sb, context );
					sb.append( " on error" );
				}
			}
			switch ( emptyBehavior ) {
				case NULL -> sb.append( " null on empty" );
				case ERROR -> sb.append( " error on empty" );
				case DEFAULT -> {
					assert emptyDefaultExpression != null;
					sb.append( " default " );
					emptyDefaultExpression.appendHqlString( sb, context );
					sb.append( " on empty" );
				}
			}
		}

		@Override
		public int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes) {
			componentNames[offset] = name;
			componentTypes[offset] = type.getNodeType();
			return 1;
		}

		@Override
		public ErrorBehavior getErrorBehavior() {
			return errorBehavior;
		}

		@Override
		public EmptyBehavior getEmptyBehavior() {
			return emptyBehavior;
		}

		@Override
		public @Nullable JpaExpression<X> getErrorDefault() {
			return errorDefaultExpression;
		}

		@Override
		public @Nullable JpaExpression<X> getEmptyDefault() {
			return emptyDefaultExpression;
		}

		@Override
		public JpaJsonValueNode<X> unspecifiedOnError() {
			this.errorDefaultExpression = null;
			this.errorBehavior = ErrorBehavior.UNSPECIFIED;
			return this;
		}

		@Override
		public JpaJsonValueNode<X> errorOnError() {
			this.errorDefaultExpression = null;
			this.errorBehavior = ErrorBehavior.ERROR;
			return this;
		}

		@Override
		public JpaJsonValueNode<X> nullOnError() {
			this.errorDefaultExpression = null;
			this.errorBehavior = ErrorBehavior.NULL;
			return this;
		}

		@Override
		public JpaJsonValueNode<X> defaultOnError(Expression<?> expression) {
			//noinspection unchecked
			this.errorDefaultExpression = (SqmExpression<X>) expression;
			this.errorBehavior = ErrorBehavior.DEFAULT;
			return this;
		}

		@Override
		public JpaJsonValueNode<X> unspecifiedOnEmpty() {
			this.emptyDefaultExpression = null;
			this.emptyBehavior = EmptyBehavior.UNSPECIFIED;
			return this;
		}

		@Override
		public JpaJsonValueNode<X> errorOnEmpty() {
			this.emptyDefaultExpression = null;
			this.emptyBehavior = EmptyBehavior.ERROR;
			return this;
		}

		@Override
		public JpaJsonValueNode<X> nullOnEmpty() {
			this.emptyDefaultExpression = null;
			this.emptyBehavior = EmptyBehavior.NULL;
			return this;
		}

		@Override
		public JpaJsonValueNode<X> defaultOnEmpty(Expression<?> expression) {
			//noinspection unchecked
			this.emptyDefaultExpression = (SqmExpression<X>) expression;
			this.emptyBehavior = EmptyBehavior.DEFAULT;
			return this;
		}
	}

	record OrdinalityColumnDefinition(String name, BasicType<Long> type) implements ColumnDefinition {
		@Override
		public ColumnDefinition copy(SqmCopyContext context) {
			return this;
		}

		@Override
		public JsonTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker) {
			return new JsonTableOrdinalityColumnDefinition( name );
		}

		@Override
		public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
			sb.append( name );
			sb.append( " for ordinality" );
		}

		@Override
		public int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes) {
			componentNames[offset] = name;
			componentTypes[offset] = type;
			return 1;
		}
	}

	static sealed class NestedColumns implements ColumnDefinition, JpaJsonTableColumnsNode {
		protected final String jsonPath;
		protected final SqmJsonTableFunction<?> table;
		protected final ArrayList<ColumnDefinition> columnDefinitions;

		NestedColumns(String jsonPath, SqmJsonTableFunction<?> table) {
			this( jsonPath, table, new ArrayList<>() );
		}

		private NestedColumns(String jsonPath, SqmJsonTableFunction<?> table, ArrayList<ColumnDefinition> columnDefinitions) {
			this.jsonPath = jsonPath;
			this.table = table;
			this.columnDefinitions = columnDefinitions;
		}

		@Override
		public int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes) {
			int i = 0;
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				i += columnDefinition.populateTupleType(offset + i, componentNames, componentTypes );
			}
			return i;
		}

		@Override
		public NestedColumns copy(SqmCopyContext context) {
			final ArrayList<ColumnDefinition> definitions = new ArrayList<>( columnDefinitions.size() );
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				definitions.add( columnDefinition.copy( context ) );
			}
			return new NestedColumns( jsonPath, context.getCopy( table ), definitions );
		}

		@Override
		public JsonTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker) {
			final List<JsonTableColumnDefinition> nestedColumns = new ArrayList<>( columnDefinitions.size() );
			for ( ColumnDefinition column : columnDefinitions ) {
				nestedColumns.add( column.convertToSqlAst( walker ) );
			}
			return new JsonTableNestedColumnDefinition( jsonPath, new JsonTableColumnsClause( nestedColumns ) );
		}

		@Override
		public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
			sb.append( "nested " );
			QuotingHelper.appendSingleQuoteEscapedString( sb, jsonPath );
			appendColumnsToHqlString( sb, context );
		}

		void appendColumnsToHqlString(StringBuilder sb, SqmRenderContext context) {
			String separator = " columns (";
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				sb.append( separator );
				columnDefinition.appendHqlString( sb, context );
				separator = ", ";
			}
			sb.append( ')' );
		}


		@Override
		public JpaJsonExistsNode existsColumn(String columnName) {
			return existsColumn( columnName, null );
		}

		@Override
		public JpaJsonExistsNode existsColumn(String columnName, @Nullable String jsonPath) {
			final BasicType<Boolean> type = table.nodeBuilder().getBooleanType();
			table.addColumn( columnName );
			final ExistsColumnDefinition existsColumnDefinition = new ExistsColumnDefinition( columnName, type, jsonPath );
			columnDefinitions.add( existsColumnDefinition );
			return existsColumnDefinition;
		}

		@Override
		public JpaJsonQueryNode queryColumn(String columnName) {
			return queryColumn( columnName, null );
		}

		@Override
		public JpaJsonQueryNode queryColumn(String columnName, @Nullable String jsonPath) {
			final BasicType<String> type = table.nodeBuilder().getTypeConfiguration().getBasicTypeRegistry()
					.resolve( String.class, SqlTypes.JSON );
			table.addColumn( columnName );
			final QueryColumnDefinition queryColumnDefinition = new QueryColumnDefinition( columnName, type, jsonPath );
			columnDefinitions.add( queryColumnDefinition );
			return queryColumnDefinition;
		}

		@Override
		public <X> JpaJsonValueNode<X> valueColumn(String columnName, Class<X> type) {
			return valueColumn( columnName, type, null );
		}

		@Override
		public <X> JpaJsonValueNode<X> valueColumn(String columnName, Class<X> type, String jsonPath) {
			return valueColumn( columnName, table.nodeBuilder().castTarget( type ), jsonPath );
		}

		@Override
		public <X> JpaJsonValueNode<X> valueColumn(String columnName, JpaCastTarget<X> type) {
			return valueColumn( columnName, type, null );
		}

		@Override
		public <X> JpaJsonValueNode<X> valueColumn(String columnName, JpaCastTarget<X> type, String jsonPath) {
			final SqmCastTarget<?> sqmCastTarget = (SqmCastTarget<?>) type;
			table.addColumn( columnName );
			final ValueColumnDefinition<X> valueColumnDefinition = new ValueColumnDefinition<>(
					columnName,
					sqmCastTarget,
					jsonPath
			);
			columnDefinitions.add( valueColumnDefinition );
			return valueColumnDefinition;
		}

		@Override
		public JpaJsonTableColumnsNode nested(String jsonPath) {
			table.checkTypeResolved();
			final NestedColumns nestedColumnDefinition = new NestedColumns( jsonPath, table );
			columnDefinitions.add( nestedColumnDefinition );
			return nestedColumnDefinition;
		}

		@Override
		public JpaJsonTableColumnsNode ordinalityColumn(String columnName) {
			final BasicType<Long> type = table.nodeBuilder().getLongType();
			table.addColumn( columnName );
			columnDefinitions.add( new OrdinalityColumnDefinition( columnName, type ) );
			return this;
		}

		public <X> X accept(SemanticQueryWalker<X> walker) {
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				if ( columnDefinition instanceof SqmJsonTableFunction.ValueColumnDefinition<?> definition ) {
					if ( definition.emptyDefaultExpression != null ) {
						definition.emptyDefaultExpression.accept( walker );
					}
					if ( definition.errorDefaultExpression != null ) {
						definition.errorDefaultExpression.accept( walker );
					}
				}
				else if ( columnDefinition instanceof NestedColumns nestedColumns ) {
					nestedColumns.accept( walker );
				}
			}

			// No-op since this object is going to be visible as function argument
			return null;
		}
	}

	public static final class Columns extends NestedColumns implements SqmTypedNode<Object> {

		public Columns(SqmJsonTableFunction<?> table) {
			super( "", table );
		}

		private Columns(SqmJsonTableFunction<?> table, ArrayList<ColumnDefinition> columnDefinitions) {
			super( "", table, columnDefinitions );
		}

		public AnonymousTupleType<?> createTupleType() {
			if ( table.columnNames.isEmpty() ) {
				throw new IllegalArgumentException( "Couldn't determine types of columns of function 'json_table'" );
			}
			final SqmBindableType<?>[] componentTypes = new SqmBindableType<?>[table.columnNames.size()];
			final String[] componentNames = new String[table.columnNames.size()];
			int result = populateTupleType( 0, componentNames, componentTypes );

			// Sanity check
			assert result == componentTypes.length;

			return new AnonymousTupleType<>( componentTypes, componentNames );
		}

		@Override
		public Columns copy(SqmCopyContext context) {
			final ArrayList<ColumnDefinition> definitions = new ArrayList<>( columnDefinitions.size() );
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				definitions.add( columnDefinition.copy( context ) );
			}
			return new Columns( context.getCopy( table ), definitions );
		}

		@Override
		public @Nullable SqmBindableType<Object> getNodeType() {
			return null;
		}

		@Override
		public NodeBuilder nodeBuilder() {
			return table.nodeBuilder();
		}

		@Override
		public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
			appendColumnsToHqlString( hql, context );
		}
	}
}
