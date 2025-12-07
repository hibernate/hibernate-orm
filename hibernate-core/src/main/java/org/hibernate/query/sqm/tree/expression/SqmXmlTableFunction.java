/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.query.criteria.JpaCastTarget;
import org.hibernate.query.criteria.JpaXmlTableColumnNode;
import org.hibernate.query.criteria.JpaXmlTableFunction;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCacheable;
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
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.XmlTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableValueColumnDefinition;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @since 7.0
 */
public class SqmXmlTableFunction<T> extends SelfRenderingSqmSetReturningFunction<T> implements JpaXmlTableFunction {

	private final Columns columns;

	public SqmXmlTableFunction(
			SqmSetReturningFunctionDescriptor descriptor,
			SetReturningFunctionRenderer renderer,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver setReturningTypeResolver,
			NodeBuilder nodeBuilder,
			SqmExpression<String> xpath,
			SqmExpression<?> document) {
		this(
				descriptor,
				renderer,
				createArgumentsList( xpath, document ),
				argumentsValidator,
				setReturningTypeResolver,
				nodeBuilder,
				new ArrayList<>()
		);
	}

	// Need to suppress some Checker Framework errors, because passing the `this` reference is unsafe,
	// though we make it safe by not calling any methods on it until initialization finishes
	@SuppressWarnings({"uninitialized", "assignment", "argument"})
	private SqmXmlTableFunction(
			SqmSetReturningFunctionDescriptor descriptor,
			SetReturningFunctionRenderer renderer,
			List<SqmTypedNode<?>> arguments,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver setReturningTypeResolver,
			NodeBuilder nodeBuilder,
			ArrayList<ColumnDefinition> columnDefinitions) {
		super( descriptor, renderer, arguments, argumentsValidator, setReturningTypeResolver, nodeBuilder, "xmltable" );
		this.columns = new Columns( this, columnDefinitions );
		arguments.set( arguments.size() - 1, this.columns );
	}

	private static List<SqmTypedNode<?>> createArgumentsList(SqmExpression<String> xpath, SqmExpression<?> document) {
		// Since the last argument is the Columns object, though that needs the `this` reference,
		// we need to construct an array with a null slot at the end, where the Columns instance is put into.
		// Suppress nullness checks as this will eventually turn non-nullable
		@SuppressWarnings("nullness")
		final SqmTypedNode<?>[] array = new SqmTypedNode[] {xpath, document, null};
		return Arrays.asList( array );
	}

	@Override
	public SqmXmlTableFunction<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<? extends SqmTypedNode<?>> arguments = getArguments();
		final List<SqmTypedNode<?>> argumentsCopy = new ArrayList<>( arguments.size() );
		for ( int i = 0; i < arguments.size() - 1; i++ ) {
			argumentsCopy.add( arguments.get( i ).copy( context ) );
		}
		final SqmXmlTableFunction<T> tableFunction = new SqmXmlTableFunction<>(
				getFunctionDescriptor(),
				getFunctionRenderer(),
				argumentsCopy,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				nodeBuilder(),
				columns.columnDefinitions
		);
		context.registerCopy( this, tableFunction );
		tableFunction.columns.columnDefinitions.ensureCapacity( columns.columnDefinitions.size() );
		for ( ColumnDefinition columnDefinition : columns.columnDefinitions ) {
			tableFunction.columns.columnDefinitions.add( columnDefinition.copy( context ) );
		}
		return tableFunction;
	}

	@Override
	protected List<SqlAstNode> resolveSqlAstArguments(List<? extends SqmTypedNode<?>> sqmArguments, SqmToSqlAstConverter walker) {
		// The last argument is the SqmXmlTableFunction.Columns which will convert to null, so remove that
		final List<SqlAstNode> sqlAstNodes = super.resolveSqlAstArguments( sqmArguments, 0, sqmArguments.size() - 1, walker );

		final List<XmlTableColumnDefinition> definitions = new ArrayList<>( columns.columnDefinitions.size() );
		for ( ColumnDefinition columnDefinition : columns.columnDefinitions ) {
			definitions.add( columnDefinition.convertToSqlAst( walker ) );
		}
		sqlAstNodes.add( new XmlTableColumnsClause( definitions ) );
		return sqlAstNodes;
	}

	@Override
	public JpaXmlTableColumnNode<String> queryColumn(String columnName) {
		return queryColumn( columnName, null );
	}

	@Override
	public JpaXmlTableColumnNode<String> queryColumn(String columnName, @Nullable String xpath) {
		final QueryColumnDefinition definition = new QueryColumnDefinition(
				this,
				columnName,
				nodeBuilder().getTypeConfiguration().getBasicTypeRegistry().resolve( String.class, SqlTypes.SQLXML ),
				xpath
		);
		columns.addColumn( definition );
		return definition;
	}

	@Override
	public <X> JpaXmlTableColumnNode<X> valueColumn(String columnName, Class<X> type) {
		return valueColumn( columnName, type, null );
	}

	@Override
	public <X> JpaXmlTableColumnNode<X> valueColumn(String columnName, JpaCastTarget<X> castTarget) {
		return valueColumn( columnName, castTarget, null );
	}

	@Override
	public <X> JpaXmlTableColumnNode<X> valueColumn(String columnName, Class<X> type, @Nullable String xpath) {
		return valueColumn( columnName, nodeBuilder().castTarget( type ), xpath );
	}

	@Override
	public <X> JpaXmlTableColumnNode<X> valueColumn(String columnName, JpaCastTarget<X> castTarget, @Nullable String xpath) {
		final ValueColumnDefinition<X> definition = new ValueColumnDefinition<>(
				this,
				columnName,
				(SqmCastTarget<X>) castTarget,
				xpath
		);
		columns.addColumn( definition );
		return definition;
	}

	@Override
	public SqmXmlTableFunction<T> ordinalityColumn(String columnName) {
		columns.addColumn( new OrdinalityColumnDefinition( columnName, nodeBuilder().getLongType() ) );
		return this;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "xmltable(" );
		getArguments().get( 0 ).appendHqlString( hql, context );
		hql.append( " passing " );
		getArguments().get( 1 ).appendHqlString( hql, context );
		columns.appendHqlString( hql, context );
		hql.append( ')' );
	}

	private void checkTypeResolved() {
		if ( isTypeResolved() ) {
			throw new IllegalStateException(
					"Type for xmltable function is already resolved. Mutation is not allowed anymore" );
		}
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmXmlTableFunction<?> that
			&& super.isCompatible( object )
			&& columns.isCompatible( that.columns );
	}

	@Override
	public int cacheHashCode() {
		int result = super.cacheHashCode();
		result = 31 * result + columns.cacheHashCode();
		return result;
	}

	sealed interface ColumnDefinition extends SqmCacheable {

		String name();

		ColumnDefinition copy(SqmCopyContext context);

		XmlTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker);

		void appendHqlString(StringBuilder sb, SqmRenderContext context);

		int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes);
	}

	static final class QueryColumnDefinition implements ColumnDefinition, JpaXmlTableColumnNode<String> {
		private final SqmXmlTableFunction<?> table;
		private final String name;
		private final BasicType<String> type;
		private final @Nullable String xpath;
		private @Nullable SqmExpression<String> defaultExpression;

		QueryColumnDefinition(SqmXmlTableFunction<?> table, String name, BasicType<String> type, @Nullable String xpath) {
			this.table = table;
			this.name = name;
			this.type = type;
			this.xpath = xpath;
		}

		private QueryColumnDefinition(SqmXmlTableFunction<?> table, String name, BasicType<String> type, @Nullable String xpath, @Nullable SqmExpression<String> defaultExpression) {
			this.table = table;
			this.name = name;
			this.type = type;
			this.xpath = xpath;
			this.defaultExpression = defaultExpression;
		}

		@Override
		public ColumnDefinition copy(SqmCopyContext context) {
			return new QueryColumnDefinition(
					table.copy( context ),
					name,
					type,
					xpath,
					defaultExpression == null ? null : defaultExpression.copy( context )
			);
		}

		@Override
		public XmlTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker) {
			return new XmlTableQueryColumnDefinition(
					name,
					type,
					xpath,
					defaultExpression == null
							? null
							: (org.hibernate.sql.ast.tree.expression.Expression) defaultExpression.accept( walker )
			);
		}

		@Override
		public JpaXmlTableColumnNode<String> defaultValue(String value) {
			return defaultExpression( table.nodeBuilder().value( value ) );
		}

		@Override
		public JpaXmlTableColumnNode<String> defaultExpression(Expression<String> expression) {
			table.checkTypeResolved();
			this.defaultExpression = (SqmExpression<String>) expression;
			return this;
		}

		@Override
		public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
			sb.append( name );
			sb.append( " xml" );
			if ( xpath != null ) {
				sb.append( " path " );
				QuotingHelper.appendSingleQuoteEscapedString( sb, xpath );
			}
			final var defaultExpression = this.defaultExpression;
			if ( defaultExpression != null ) {
				sb.append( " default " );
				defaultExpression.appendHqlString( sb, context );
			}
		}

		@Override
		public int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes) {
			componentNames[offset] = name;
			componentTypes[offset] = type;
			return 1;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean isCompatible(Object object) {
			return object instanceof QueryColumnDefinition that
				&& name.equals( that.name )
				&& type.equals( that.type )
				&& Objects.equals( xpath, that.xpath )
				&& SqmCacheable.areCompatible( defaultExpression, that.defaultExpression );
		}

		@Override
		public int cacheHashCode() {
			int result = name.hashCode();
			result = 31 * result + type.hashCode();
			result = 31 * result + Objects.hashCode( xpath );
			result = 31 * result + SqmCacheable.cacheHashCode( defaultExpression );
			return result;
		}

	}

	static final class ValueColumnDefinition<X> implements ColumnDefinition, JpaXmlTableColumnNode<X> {
		private final SqmXmlTableFunction<?> table;
		private final String name;
		private final SqmCastTarget<X> type;
		private final @Nullable String xpath;
		private @Nullable SqmExpression<X> defaultExpression;

		ValueColumnDefinition(SqmXmlTableFunction<?> table, String name, SqmCastTarget<X> type, @Nullable String xpath) {
			this.table = table;
			this.name = name;
			this.type = type;
			this.xpath = xpath;
		}

		private ValueColumnDefinition(SqmXmlTableFunction<?> table, String name, SqmCastTarget<X> type, @Nullable String xpath, @Nullable SqmExpression<X> defaultExpression) {
			this.table = table;
			this.name = name;
			this.type = type;
			this.xpath = xpath;
			this.defaultExpression = defaultExpression;
		}

		@Override
		public ColumnDefinition copy(SqmCopyContext context) {
			return new ValueColumnDefinition<>(
					table.copy( context ),
					name,
					type,
					xpath,
					defaultExpression == null ? null : defaultExpression.copy( context )
			);
		}

		@Override
		public XmlTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker) {
			return new XmlTableValueColumnDefinition(
					name,
					(CastTarget) type.accept( walker ),
					xpath,
					defaultExpression == null
							? null
							: (org.hibernate.sql.ast.tree.expression.Expression) defaultExpression.accept( walker )
			);
		}

		@Override
		public JpaXmlTableColumnNode<X> defaultValue(X value) {
			return defaultExpression( table.nodeBuilder().value( value ) );
		}

		@Override
		public JpaXmlTableColumnNode<X> defaultExpression(Expression<X> expression) {
			table.checkTypeResolved();
			this.defaultExpression = (SqmExpression<X>) expression;
			return this;
		}

		@Override
		public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
			sb.append( name );
			sb.append( ' ' );
			type.appendHqlString( sb, context );
			if ( xpath != null ) {
				sb.append( " path " );
				QuotingHelper.appendSingleQuoteEscapedString( sb, xpath );
			}
			final var defaultExpression = this.defaultExpression;
			if ( defaultExpression != null ) {
				sb.append( " default " );
				defaultExpression.appendHqlString( sb, context );
			}
		}

		@Override
		public int populateTupleType(int offset, String[] componentNames, SqmExpressible<?>[] componentTypes) {
			componentNames[offset] = name;
			componentTypes[offset] = castNonNull( type.getNodeType() );
			return 1;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean isCompatible(Object object) {
			return object instanceof ValueColumnDefinition<?> that
				&& name.equals( that.name )
				&& type.equals( that.type )
				&& Objects.equals( xpath, that.xpath )
				&& SqmCacheable.areCompatible( defaultExpression, that.defaultExpression );
		}

		@Override
		public int cacheHashCode() {
			int result = name.hashCode();
			result = 31 * result + type.hashCode();
			result = 31 * result + Objects.hashCode( xpath );
			result = 31 * result + SqmCacheable.cacheHashCode( defaultExpression );
			return result;
		}

	}

	record OrdinalityColumnDefinition(String name, BasicType<Long> type) implements ColumnDefinition {
		@Override
		public ColumnDefinition copy(SqmCopyContext context) {
			return this;
		}

		@Override
		public XmlTableColumnDefinition convertToSqlAst(SqmToSqlAstConverter walker) {
			return new XmlTableOrdinalityColumnDefinition( name );
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

		@Override
		public boolean isCompatible(Object object) {
			return equals( object );
		}

		@Override
		public int cacheHashCode() {
			return hashCode();
		}
	}

	public static final class Columns implements SqmTypedNode<Object> {

		private final SqmXmlTableFunction<?> table;
		private final Set<String> columnNames;
		private final ArrayList<ColumnDefinition> columnDefinitions;

		private Columns(SqmXmlTableFunction<?> table, ArrayList<ColumnDefinition> columnDefinitions) {
			this.table = table;
			this.columnDefinitions = columnDefinitions;
			this.columnNames = new HashSet<>( columnDefinitions.size() );
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				columnNames.add( columnDefinition.name() );
			}
		}

		public AnonymousTupleType<?> createTupleType() {
			if ( columnDefinitions.isEmpty() ) {
				throw new IllegalArgumentException( "Couldn't determine types of columns of function 'xmltable'" );
			}
			final SqmBindableType<?>[] componentTypes = new SqmBindableType<?>[columnDefinitions.size()];
			final String[] componentNames = new String[columnDefinitions.size()];
			int offset = 0;
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				offset += columnDefinition.populateTupleType( offset, componentNames, componentTypes );
			}

			// Sanity check
			assert offset == componentTypes.length;

			return new AnonymousTupleType<>( componentTypes, componentNames );
		}

		@Override
		public Columns copy(SqmCopyContext context) {
			final ArrayList<ColumnDefinition> definitions = new ArrayList<>( columnDefinitions.size() );
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				definitions.add( columnDefinition.copy( context ) );
			}
			return new Columns( castNonNull( context.getCopy( table ) ), definitions );
		}

		private void addColumn(ColumnDefinition columnDefinition) {
			table.checkTypeResolved();
			if ( !columnNames.add( columnDefinition.name() ) ) {
				throw new IllegalStateException( "Duplicate column: " + columnDefinition.name() );
			}
			columnDefinitions.add( columnDefinition );
		}

		@Override
		public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
			String separator = " columns ";
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				hql.append( separator );
				columnDefinition.appendHqlString( hql, context );
				separator = ", ";
			}
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
		public <X> X accept(SemanticQueryWalker<X> walker) {
			for ( ColumnDefinition columnDefinition : columnDefinitions ) {
				if ( columnDefinition instanceof SqmXmlTableFunction.ValueColumnDefinition<?> definition ) {
					if ( definition.defaultExpression != null ) {
						definition.defaultExpression.accept( walker );
					}
				}
				else if ( columnDefinition instanceof SqmXmlTableFunction.QueryColumnDefinition definition ) {
					if ( definition.defaultExpression != null ) {
						definition.defaultExpression.accept( walker );
					}
				}
			}

			// This is fine since this object is going to be visible as function argument only for logging purposes

			//noinspection unchecked
			return (X) this;
		}

		@Override
		public boolean equals(@Nullable Object object) {
			return object instanceof Columns that
				&& Objects.equals( columnDefinitions, that.columnDefinitions );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( columnDefinitions );
		}

		@Override
		public boolean isCompatible(Object object) {
			return object instanceof Columns that
				&& SqmCacheable.areCompatible( columnDefinitions, that.columnDefinitions );
		}

		@Override
		public int cacheHashCode() {
			return SqmCacheable.cacheHashCode( columnDefinitions );
		}
	}
}
