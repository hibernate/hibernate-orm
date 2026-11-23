package org.hibernate.action.queue.internal.exec;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
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

import static org.hibernate.query.sqm.ComparisonOperator.EQUAL;
import static org.hibernate.type.StandardBasicTypes.INTEGER;

/// The unfetched foreign-key columns supplied by one associated row, identified by its primary key.
record PropertyRefLookup(EntityIdentifierMapping id, ParameterUsage usage, List<Column> columns) {
	record Column(ColumnValueBinding binding, SelectableMapping source, SelectableMapping target) {}

	static final class IdentifierParameter extends ColumnValueParameter {
		final int component;

		IdentifierParameter(ColumnReference reference, ParameterUsage usage, int component) {
			super( reference, usage );
			this.component = component;
		}
	}

	private record Source(StandardTableGroup table, Junction restriction, List<ColumnValueParameter> parameters) {
		void addTo(QuerySpec query) {
			query.getFromClause().addRoot( table );
			query.applyPredicate( restriction );
		}

		SelectStatement select(Expression expression) {
			final var query = new QuerySpec( false, 1 );
			addTo( query );
			query.getSelectClause().addSqlSelection( new SqlSelectionImpl( expression ) );
			return new SelectStatement( query );
		}

		ColumnReference reference(Column column) {
			return new ColumnReference( table.getPrimaryTableReference(), column.target() );
		}
	}

	private Source source(String alias, SessionFactoryImplementor factory) {
		final var table = new NamedTableReference( id.getContainingTableExpression(), alias );
		final var entity = id.findContainingEntityMapping();
		final var group = new StandardTableGroup( true, new NavigablePath( entity.getEntityName(), alias ),
				entity, alias, table, null, factory );
		final var restriction = new Junction();
		final List<ColumnValueParameter> parameters = new ArrayList<>( id.getJdbcTypeCount() );
		// The row bindings store the decomposed identifier under the first unfetched column.
		final String columnName = columns.get( 0 ).source().getSelectionExpression();
		for ( int i = 0; i < id.getJdbcTypeCount(); i++ ) {
			final var selectable = id.getSelectable( i );
			final var reference = new ColumnReference( (String) null, columnName, false, null, selectable.getJdbcMapping() );
			final var parameter = new IdentifierParameter( reference, usage, i );
			parameters.add( parameter );
			restriction.add( new ComparisonPredicate( new ColumnReference( table, selectable ), EQUAL, parameter ) );
		}
		return new Source( group, restriction, parameters );
	}

	static TableInsertStandard insert(TableInsertStandard insert, List<PropertyRefLookup> lookups,
			SessionFactoryImplementor factory) {
		final var query = new QuerySpec( true, lookups.size() );
		final List<ColumnValueParameter> restrictions = new ArrayList<>();
		final Map<ColumnValueBinding, Expression> selections = new IdentityHashMap<>();
		for ( int i = 0; i < lookups.size(); i++ ) {
			final var lookup = lookups.get( i );
			final var source = lookup.source( "uk" + i, factory );
			source.addTo( query );
			restrictions.addAll( source.parameters() );
			for ( var column : lookup.columns ) {
				selections.put( column.binding(), source.reference( column ) );
			}
		}
		final List<ColumnValueBinding> values = new ArrayList<>();
		final List<ColumnValueParameter> parameters = new ArrayList<>();
		for ( var binding : insert.getValueBindings() ) {
			final var expression = binding.getValueExpression();
			// Omitting a DEFAULT target column applies its default to an INSERT ... SELECT.
			if ( !"default".equalsIgnoreCase( expression.getFragment() ) ) {
				values.add( binding );
				final var selection = selections.get( binding );
				if ( selection == null ) {
					parameters.addAll( expression.getParameters() );
				}
				query.getSelectClause().addSqlSelection( new SqlSelectionImpl( selection == null ? expression : selection ) );
			}
		}
		parameters.addAll( restrictions );
		return new TableInsertStandard( insert, values, new SelectStatement( query ), parameters );
	}

	static TableUpdateStandard update(TableUpdateStandard update, List<PropertyRefLookup> lookups,
			SessionFactoryImplementor factory) {
		final Map<ColumnValueBinding, ColumnValueBinding> replacements = new IdentityHashMap<>();
		final var checks = new Junction();
		final List<ColumnValueParameter> checkParameters = new ArrayList<>();
		final var one = new QueryLiteral<>( 1, factory.getTypeConfiguration().getBasicTypeRegistry().resolve( INTEGER ) );
		for ( var lookup : lookups ) {
			final var source = lookup.source( "uk", factory );
			checkParameters.addAll( source.parameters() );
			checks.add( new ExistsPredicate( source.select( one ), false, null ) );
			for ( var column : lookup.columns ) {
				final var expression = new ColumnWriteFragment( source.parameters(), column.source(),
						source.select( source.reference( column ) ) );
				replacements.put( column.binding(), new ColumnValueBinding( column.binding().getColumnReference(), expression ) );
			}
		}
		return new TableUpdateStandard( update, substitute( update.getValueBindings(), replacements ),
				substitute( update.getKeyBindings(), replacements ), checks, checkParameters );
	}

	private static List<ColumnValueBinding> substitute(List<ColumnValueBinding> bindings,
			Map<ColumnValueBinding, ColumnValueBinding> replacements) {
		return bindings.stream().map( binding -> replacements.getOrDefault( binding, binding ) ).toList();
	}
}
