package org.hibernate.action.queue.internal.decompose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.model.TableInsertCustomSql;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdateCustomSql;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ExistsPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;

import static org.hibernate.query.sqm.ComparisonOperator.EQUAL;
import static org.hibernate.type.StandardBasicTypes.INTEGER;

/// The forms of one table mutation which read foreign keys from the rows they reference.
///
/// Each form is a distinct statement with its own [shape][StatementShapeKey], chosen
/// while the flush is planned, so that rows which share a form are batched together.
public final class KeyLookupStatements {
	/// A statement which reads keys from referenced rows.
	public record Statement(MutationOperation operation, StatementShapeKey shapeKey) {
	}

	private final TableMutation<?> mutation;
	private final TableDescriptor table;
	private final MutationKind kind;
	private final SessionFactoryImplementor factory;
	private final Map<List<KeyLookup.Shape>, Optional<Statement>> statements = new ConcurrentHashMap<>();

	/// @param mutation the form of the mutation which binds every key
	public KeyLookupStatements(
			TableMutation<?> mutation,
			TableDescriptor table,
			MutationKind kind,
			SessionFactoryImplementor factory) {
		this.mutation = mutation;
		this.table = table;
		this.kind = kind;
		this.factory = factory;
	}

	/// @return `null` if the mutation has no form which reads the given keys
	public Statement resolve(List<KeyLookup.Shape> shapes) {
		var statement = statements.get( shapes );
		if ( statement == null ) {
			statement = statements.computeIfAbsent( shapes,
					key -> Optional.ofNullable( derive( mutation, key, null, table, kind, factory ) ) );
		}
		return statement.orElse( null );
	}

	/// Derive the form of a mutation which reads the given keys.
	///
	/// @return `null` if the mutation has no such form
	public static Statement derive(
			TableMutation<?> mutation,
			List<KeyLookup.Shape> shapes,
			ValuesAnalysis valuesAnalysis,
			TableDescriptor table,
			MutationKind kind,
			SessionFactoryImplementor factory) {
		final var rewriter = new Rewriter( shapes, factory );
		mutation.accept( rewriter );
		if ( rewriter.result == null ) {
			return null;
		}
		final var operation = rewriter.result.createMutationOperation( valuesAnalysis, factory );
		return new Statement( operation, StatementShapeKey.forMutation( table.name(), kind, table, operation ) );
	}

	/// Rewrites a standard insert or update. Any other mutation is left without a result.
	private static final class Rewriter extends AbstractSqlAstWalker {
		private final List<KeyLookup.Shape> shapes;
		private final SessionFactoryImplementor factory;
		private TableMutation<?> result;

		private Rewriter(List<KeyLookup.Shape> shapes, SessionFactoryImplementor factory) {
			this.shapes = shapes;
			this.factory = factory;
		}

		// The base walker rejects these. They have no form which reads a key.

		@Override
		public void visitCustomTableInsert(TableInsertCustomSql insert) {
		}

		@Override
		public void visitCustomTableUpdate(TableUpdateCustomSql update) {
		}

		@Override
		public void visitOptionalTableUpdate(OptionalTableUpdate update) {
		}

		@Override
		public void visitStandardTableInsert(TableInsertStandard insert) {
			// A scalar subquery among the original values would move into the select list
			if ( insert.hasValueSubquery() && !factory.getJdbcServices().getDialect().getSubquerySupport()
					.supports( SubquerySupport.Feature.SELECT_LIST ) ) {
				return;
			}
			final var query = new QuerySpec( true, shapes.size() );
			final List<ColumnValueParameter> identifiers = new ArrayList<>();
			final Map<String, Expression> selections = new HashMap<>();
			for ( int i = 0; i < shapes.size(); i++ ) {
				final var shape = shapes.get( i );
				final var source = new Source( shape.lookup(), "uk" + i, factory );
				query.getFromClause().addRoot( source.table );
				query.applyPredicate( source.restriction );
				identifiers.addAll( source.parameters );
				for ( int column = 0; column < shape.lookup().columnCount(); column++ ) {
					if ( shape.isMissing( column ) ) {
						selections.put( shape.lookup().keyColumn( column ).getSelectionExpression(),
								source.reference( column ) );
					}
				}
			}
			final List<ColumnValueBinding> values = new ArrayList<>();
			final List<ColumnValueParameter> parameters = new ArrayList<>();
			for ( var binding : insert.getValueBindings() ) {
				final var expression = binding.getValueExpression();
				final var selection = selections.remove( binding.getColumnReference().getColumnExpression() );
				if ( selection != null ) {
					if ( !isParameter( expression ) ) {
						return;
					}
					values.add( binding );
					query.getSelectClause().addSqlSelection( new SqlSelectionImpl( selection ) );
				}
				// Omitting a DEFAULT target column applies its default to an INSERT ... SELECT
				else if ( !"default".equalsIgnoreCase( expression.getFragment() ) ) {
					values.add( binding );
					parameters.addAll( expression.getParameters() );
					query.getSelectClause().addSqlSelection( new SqlSelectionImpl( expression ) );
				}
			}
			if ( selections.isEmpty() ) {
				parameters.addAll( identifiers );
				result = new TableInsertStandard( insert, values, new SelectStatement( query ), parameters );
			}
		}

		@Override
		public void visitStandardTableUpdate(TableUpdateStandard update) {
			final List<ColumnValueBinding> values = new ArrayList<>( update.getValueBindings() );
			final List<ColumnValueBinding> keys = new ArrayList<>( update.getKeyBindings() );
			final var checks = new Junction();
			final List<ColumnValueParameter> identifiers = new ArrayList<>();
			final var one = new QueryLiteral<>( 1, factory.getTypeConfiguration().getBasicTypeRegistry().resolve( INTEGER ) );
			for ( var shape : shapes ) {
				final var lookup = shape.lookup();
				final var source = new Source( lookup, "uk", factory );
				identifiers.addAll( source.parameters );
				// Without this, a missing row would assign null instead of matching no row
				checks.add( new ExistsPredicate( source.select( one ), false, null ) );
				final var bindings = lookup.usage() == ParameterUsage.SET ? values : keys;
				for ( int column = 0; column < lookup.columnCount(); column++ ) {
					if ( shape.isMissing( column ) ) {
						final int position = positionOfParameter( bindings, lookup.keyColumn( column ).getSelectionExpression() );
						if ( position < 0 ) {
							return;
						}
						bindings.set( position, new ColumnValueBinding(
								bindings.get( position ).getColumnReference(),
								new ColumnWriteFragment( source.parameters, lookup.keyColumn( column ),
										source.select( source.reference( column ) ) )
						) );
					}
				}
			}
			result = new TableUpdateStandard( update, values, keys, checks, identifiers );
		}

		private static int positionOfParameter(List<ColumnValueBinding> bindings, String column) {
			for ( int i = 0; i < bindings.size(); i++ ) {
				final var binding = bindings.get( i );
				if ( column.equals( binding.getColumnReference().getColumnExpression() ) ) {
					return isParameter( binding.getValueExpression() ) ? i : -1;
				}
			}
			return -1;
		}

		private static boolean isParameter(ColumnWriteFragment expression) {
			return expression != null && "?".equals( expression.getFragment() ) && expression.getParameters().size() == 1;
		}
	}

	/// The referenced row of one lookup, identified by its primary key.
	private static final class Source {
		private final KeyLookup lookup;
		private final StandardTableGroup table;
		private final Junction restriction = new Junction();
		private final List<ColumnValueParameter> parameters;

		private Source(KeyLookup lookup, String alias, SessionFactoryImplementor factory) {
			this.lookup = lookup;
			final var identifier = lookup.identifier();
			final var entity = identifier.findContainingEntityMapping();
			final var reference = new NamedTableReference( identifier.getContainingTableExpression(), alias );
			table = new StandardTableGroup( true, new NavigablePath( entity.getEntityName(), alias ),
					entity, alias, reference, null, factory );
			parameters = new ArrayList<>( identifier.getJdbcTypeCount() );
			for ( int i = 0; i < identifier.getJdbcTypeCount(); i++ ) {
				final var column = identifier.getSelectable( i );
				final var parameter = new ColumnValueParameter(
						new ColumnReference( (String) null, lookup.parameterName( i ), false, null, column.getJdbcMapping() ),
						ParameterUsage.RESTRICT
				);
				parameters.add( parameter );
				restriction.add( new ComparisonPredicate( new ColumnReference( reference, column ), EQUAL, parameter ) );
			}
		}

		private ColumnReference reference(int column) {
			return new ColumnReference( table.getPrimaryTableReference(), lookup.targetColumn( column ) );
		}

		private SelectStatement select(Expression expression) {
			final var query = new QuerySpec( false, 1 );
			query.getFromClause().addRoot( table );
			query.applyPredicate( restriction );
			query.getSelectClause().addSqlSelection( new SqlSelectionImpl( expression ) );
			return new SelectStatement( query );
		}
	}
}
