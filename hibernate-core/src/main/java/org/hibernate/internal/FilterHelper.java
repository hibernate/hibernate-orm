/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Filter;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.type.Type;

import static org.hibernate.internal.util.StringHelper.safeInterning;

/**
 * Implementation of FilterHelper.
 *
 * @author Steve Ebersole
 * @author Rob Worsnop
 * @author Nathan Xu
 */
public class FilterHelper {

	private static final Pattern FILTER_PARAMETER_PATTERN = Pattern.compile( ":(\\S+)\\.(\\w+)" );

	private final String[] filterNames;
	private final String[] filterConditions;
	private final boolean[] filterAutoAliasFlags;
	private final Map<String, String>[] filterAliasTableMaps;

	/**
	 * The map of defined filters.  This is expected to be in format
	 * where the filter names are the map keys, and the defined
	 * conditions are the values.
	 *
	 * @param filters The map of defined filters.
	 * @param factory The session factory
	 */
	public FilterHelper(List<FilterConfiguration> filters, SessionFactoryImplementor factory) {
		int filterCount = filters.size();
		filterNames = new String[filterCount];
		filterConditions = new String[filterCount];
		filterAutoAliasFlags = new boolean[filterCount];
		filterAliasTableMaps = new Map[filterCount];
		filterCount = 0;
		for ( final FilterConfiguration filter : filters ) {
			filterAutoAliasFlags[filterCount] = false;
			filterNames[filterCount] = safeInterning( filter.getName() );
			filterConditions[filterCount] = safeInterning( filter.getCondition() );
			filterAliasTableMaps[filterCount] = filter.getAliasTableMap( factory );
			if ( ( filterAliasTableMaps[filterCount].isEmpty()
					|| isTableFromPersistentClass( filterAliasTableMaps[filterCount] ) )
					&& filter.useAutoAliasInjection() ) {
				filterConditions[filterCount] = safeInterning(
							Template.renderWhereStringTemplate(
							filter.getCondition(),
							FilterImpl.MARKER,
							factory.getDialect(),
							factory.getQueryEngine().getSqmFunctionRegistry()
					)
				);
				filterAutoAliasFlags[filterCount] = true;
			}

			filterConditions[filterCount] = safeInterning(
					StringHelper.replace(
						filterConditions[filterCount],
						":",
						":" + filterNames[filterCount] + "."
					)
			);
			filterCount++;
		}
	}

	private static boolean isTableFromPersistentClass(Map<String, String> aliasTableMap) {
		return aliasTableMap.size() == 1 && aliasTableMap.containsKey( null );
	}

	public boolean isAffectedBy(Map<String, Filter> enabledFilters) {
		for ( String filterName : filterNames ) {
			if ( enabledFilters.containsKey( filterName ) ) {
				return true;
			}
		}
		return false;
	}

	public String render(FilterAliasGenerator aliasGenerator, Map<String, Filter> enabledFilters) {
		StringBuilder buffer = new StringBuilder();
		render( buffer, aliasGenerator, enabledFilters );
		return buffer.toString();
	}

	public void render(StringBuilder buffer, FilterAliasGenerator aliasGenerator, Map<String, Filter> enabledFilters) {
		if ( CollectionHelper.isEmpty( filterNames ) ) {
			return;
		}
		for ( int i = 0, max = filterNames.length; i < max; i++ ) {
			if ( enabledFilters.containsKey( filterNames[i] ) ) {
				final String condition = filterConditions[i];
				if ( StringHelper.isNotEmpty( condition ) ) {
					if ( buffer.length() > 0 ) {
						buffer.append( " and " );
					}
					buffer.append( render( aliasGenerator, i ) );
				}
			}
		}
	}

	private String render(FilterAliasGenerator aliasGenerator, int filterIndex) {
		Map<String, String> aliasTableMap = filterAliasTableMaps[filterIndex];
		String condition = filterConditions[filterIndex];
		if ( aliasGenerator == null ) {
			return StringHelper.replace( condition, FilterImpl.MARKER + ".", "");
		}
		if ( filterAutoAliasFlags[filterIndex] ) {
			return StringHelper.replace(
					condition,
					FilterImpl.MARKER,
					aliasGenerator.getAlias( aliasTableMap.get( null ) )
			);
		}
		else if ( isTableFromPersistentClass( aliasTableMap ) ) {
			return StringHelper.replace( condition,  "{alias}", aliasGenerator.getAlias( aliasTableMap.get( null ) ) );
		}
		else {
			for ( Map.Entry<String, String> entry : aliasTableMap.entrySet() ) {
				condition = StringHelper.replace( condition,
						"{" + entry.getKey() + "}",
						aliasGenerator.getAlias( entry.getValue() )
				);
			}
			return condition;
		}
	}

	public static FilterPredicate createFilterPredicate(
			LoadQueryInfluencers loadQueryInfluencers,
			Joinable joinable,
			TableGroup rootTableGroup) {
		return createFilterPredicate( loadQueryInfluencers, joinable, rootTableGroup, true );
	}

	public static FilterPredicate createFilterPredicate(
			Map<String,Filter> enabledFilters,
			Joinable joinable,
			TableGroup rootTableGroup) {
		return createFilterPredicate( enabledFilters, joinable, rootTableGroup, true );
	}

	public static FilterPredicate createFilterPredicate(
			Map<String,Filter> enabledFilters,
			Joinable joinable,
			TableGroup rootTableGroup,
			boolean useIdentificationVariable) {
		final String filterFragment = joinable.filterFragment(
				rootTableGroup,
				enabledFilters,
				Collections.emptySet(),
				useIdentificationVariable
		);
		if ( StringHelper.isNotEmpty( filterFragment ) ) {
			return doCreateFilterPredicate( filterFragment, enabledFilters );
		}
		else {
			return null;
		}

	}

	public static FilterPredicate createFilterPredicate(
			LoadQueryInfluencers loadQueryInfluencers,
			Joinable joinable,
			TableGroup rootTableGroup,
			boolean useIdentificationVariable) {
		final String filterFragment = joinable.filterFragment(
				rootTableGroup,
				loadQueryInfluencers.getEnabledFilters(),
				Collections.emptySet(),
				useIdentificationVariable
		);
		if ( StringHelper.isNotEmpty( filterFragment ) ) {
			return doCreateFilterPredicate( filterFragment, loadQueryInfluencers.getEnabledFilters() );
		}
		else {
			return null;
		}
	}

	public static FilterPredicate createManyToManyFilterPredicate(LoadQueryInfluencers loadQueryInfluencers, CollectionPersister collectionPersister, TableGroup tableGroup) {
		assert collectionPersister.isManyToMany();
		final String filterFragment = collectionPersister.getManyToManyFilterFragment( tableGroup, loadQueryInfluencers.getEnabledFilters() );
		if ( StringHelper.isNotEmpty( filterFragment ) ) {
			return doCreateFilterPredicate( filterFragment, loadQueryInfluencers.getEnabledFilters() );
		}
		else {
			return null;
		}
	}

	public static FilterPredicate doCreateFilterPredicate(String filterFragment, Map<String, Filter> enabledFilters) {
		final Matcher matcher = FILTER_PARAMETER_PATTERN.matcher( filterFragment );
		final StringBuilder sb = new StringBuilder();
		int pos = 0;
		final List<FilterJdbcParameter> parameters = new ArrayList<>( matcher.groupCount() );
		while( matcher.find() ) {
			sb.append( filterFragment, pos, matcher.start() );
			pos = matcher.end();
			sb.append( "?" );
			final String filterName = matcher.group( 1 );
			final String parameterName = matcher.group( 2 );
			final FilterImpl enabledFilter = (FilterImpl) enabledFilters.get( filterName );
			if ( enabledFilter == null ) {
				throw new MappingException( String.format( "unknown filter [%s]", filterName ) );
			}
			final Type parameterType = enabledFilter.getFilterDefinition().getParameterType( parameterName );
			if ( ! (parameterType instanceof JdbcMapping ) ) {
				throw new MappingException( String.format( "parameter [%s] for filter [%s] is not of JdbcMapping type", parameterName, filterName ) );
			}
			final JdbcMapping jdbcMapping = (JdbcMapping) parameterType;
			final Object parameterValue = enabledFilter.getParameter( parameterName );
			if ( parameterValue == null ) {
				throw new MappingException( String.format( "unknown parameter [%s] for filter [%s]", parameterName, filterName ) );
			}
			if ( parameterValue instanceof Iterable && !jdbcMapping.getJavaTypeDescriptor().isInstance( parameterValue ) ) {
				final Iterator<?> iterator = ( (Iterable<?>) parameterValue ).iterator();
				if ( iterator.hasNext() ) {
					parameters.add( new FilterJdbcParameter( jdbcMapping, iterator.next() ) );
					while ( iterator.hasNext() ) {
						sb.append( ",?" );
						parameters.add( new FilterJdbcParameter( jdbcMapping, iterator.next() ) );
					}
				}
				else {
					// We need a dummy value if the list is empty
					parameters.add( new FilterJdbcParameter( jdbcMapping, null ) );
				}
			}
			else {
				parameters.add( new FilterJdbcParameter( jdbcMapping, parameterValue ) );
			}
		}
		sb.append( filterFragment, pos, filterFragment.length() );
		return new FilterPredicate( sb.toString(), parameters );
	}
}
