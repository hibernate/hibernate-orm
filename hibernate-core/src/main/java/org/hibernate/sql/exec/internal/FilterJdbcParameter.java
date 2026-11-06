package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import jakarta.annotation.Nullable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * A JDBC parameter for a {@linkplain org.hibernate.Filter filter} parameter,
 * or for one element of a multivalued filter parameter.
 * <p>
 * The value is never captured when the SQL is translated, so the same
 * translated SQL can be shared by sessions with different filter arguments.
 * The binder takes the value from the {@link JdbcParameterBindings} of the
 * execution, or else from the filter enabled in the executing session.
 */
public final class FilterJdbcParameter implements JdbcParameter, JdbcParameterBinder {
	private final String filterName;
	private final String parameterName;
	private final JdbcMapping jdbcMapping;
	private final int elementIndex;
	private final int elementCount;

	private FilterJdbcParameter(
			String filterName,
			String parameterName,
			JdbcMapping jdbcMapping,
			int elementIndex,
			int elementCount) {
		this.filterName = filterName;
		this.parameterName = parameterName;
		this.jdbcMapping = jdbcMapping;
		this.elementIndex = elementIndex;
		this.elementCount = elementCount;
	}

	/**
	 * Create the JDBC parameters for a filter parameter, one for each
	 * element if the given value is multivalued.
	 */
	public static List<FilterJdbcParameter> create(
			String filterName,
			String parameterName,
			JdbcMapping jdbcMapping,
			@Nullable Object value) {
		if ( isMultiValued( value, jdbcMapping ) ) {
			final int count = elementCount( value );
			final List<FilterJdbcParameter> parameters = arrayList( count );
			for ( int i = 0; i < count; i++ ) {
				parameters.add( new FilterJdbcParameter( filterName, parameterName, jdbcMapping, i, count ) );
			}
			return parameters;
		}
		else {
			return List.of( new FilterJdbcParameter( filterName, parameterName, jdbcMapping, -1, -1 ) );
		}
	}

	/**
	 * Whether the given filter argument expands to a list of JDBC parameters.
	 */
	public static boolean isMultiValued(@Nullable Object value, JdbcMapping jdbcMapping) {
		return value instanceof Iterable<?>
			&& !jdbcMapping.getJavaTypeDescriptor().isInstance( value );
	}

	/**
	 * The number of elements of a multivalued filter argument.
	 */
	public static int elementCount(Object value) {
		if ( value instanceof Collection<?> collection ) {
			return collection.size();
		}
		int count = 0;
		for ( Iterator<?> iterator = ((Iterable<?>) value).iterator(); iterator.hasNext(); iterator.next() ) {
			count++;
		}
		return count;
	}

	/**
	 * Add a binding for each filter parameter of the given operation, with the
	 * JDBC value from the filters enabled in the given session. The bindings keep
	 * the values of this execution, for example for subselect fetching.
	 */
	public static void bindFilterParameters(
			JdbcOperation operation,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		if ( session.getLoadQueryInfluencers().hasEnabledFilters() ) {
			for ( var binder : operation.getParameterBinders() ) {
				if ( binder instanceof FilterJdbcParameter parameter
						&& jdbcParameterBindings.getBinding( parameter ) == null ) {
					jdbcParameterBindings.addBinding(
							parameter,
							new JdbcParameterBindingImpl( parameter.jdbcMapping, parameter.resolveJdbcValue( session ) )
					);
				}
			}
		}
	}

	public String getFilterName() {
		return filterName;
	}

	public String getParameterName() {
		return parameterName;
	}

	/**
	 * The value of this parameter from the filter enabled in the given session.
	 */
	public @Nullable Object resolveValue(SharedSessionContractImplementor session) {
		final var filter = session.getLoadQueryInfluencers().getEnabledFilter( filterName );
		if ( filter == null ) {
			throw new HibernateException( "Filter '" + filterName
					+ "' is not enabled in the session which executes a statement restricted by it" );
		}
		final Object value = filter.getParameterValue( parameterName );
		final boolean multiValued = isMultiValued( value, jdbcMapping );
		if ( multiValued != elementIndex >= 0
				|| multiValued && elementCount( value ) != elementCount ) {
			throw new HibernateException( "Argument of filter parameter '" + filterName + "." + parameterName
					+ "' does not have the number of elements that the statement was translated for" );
		}
		return multiValued ? element( (Iterable<?>) value, elementIndex ) : value;
	}

	private @Nullable Object resolveJdbcValue(SharedSessionContractImplementor session) {
		return jdbcMapping.convertToRelationalValue( resolveValue( session ) );
	}

	private static Object element(Iterable<?> iterable, int index) {
		if ( iterable instanceof List<?> list ) {
			return list.get( index );
		}
		final var iterator = iterable.iterator();
		for ( int i = 0; i < index; i++ ) {
			iterator.next();
		}
		return iterator.next();
	}

	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) throws SQLException {
		final var session = executionContext.getSession();
		final var binding = jdbcParameterBindings.getBinding( this );
		jdbcMapping.getJdbcValueBinder().bind(
				statement,
				binding == null ? resolveJdbcValue( session ) : binding.getBindValue(),
				startPosition,
				session
		);
	}

	@Override
	public JdbcParameterBinder getParameterBinder() {
		return this;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return jdbcMapping;
	}

	@Override
	public @Nullable Integer getParameterId() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new IllegalStateException();
	}
}
