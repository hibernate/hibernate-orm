/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Filter;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.Restrictable;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;

import static org.hibernate.internal.util.StringHelper.safeInterning;

/**
 * Implementation of FilterHelper.
 *
 * @author Steve Ebersole
 * @author Rob Worsnop
 * @author Nathan Xu
 */
public class FilterHelper {
	private static final Pattern FILTER_PARAMETER_PATTERN = Pattern.compile( ":(\\S+)(\\w+)" );

	private final String[] filterNames;
	private final String[] filterConditions;
	private final boolean[] filterAutoAliasFlags;
	private final Map<String, String>[] filterAliasTableMaps;
	private final List<String>[] parameterNames;

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
		parameterNames = new List[filterCount];

		filterCount = 0;
		for ( final FilterConfiguration filter : filters ) {
			final String filterName = safeInterning( filter.getName() );
			filterNames[filterCount] = filterName;
			filterConditions[filterCount] = safeInterning( filter.getCondition() );

			filterAliasTableMaps[filterCount] = filter.getAliasTableMap( factory );
			filterAutoAliasFlags[filterCount] = false;

			if ( ( filterAliasTableMaps[filterCount].isEmpty()
					|| isTableFromPersistentClass( filterAliasTableMaps[filterCount] ) )
					&& filter.useAutoAliasInjection() ) {
				final String autoAliasedCondition = Template.renderWhereStringTemplate(
						filter.getCondition(),
						FilterImpl.MARKER,
						factory.getJdbcServices().getDialect(),
						factory.getTypeConfiguration(),
						factory.getQueryEngine().getSqmFunctionRegistry()
				);
				filterConditions[filterCount] = safeInterning( autoAliasedCondition );
				filterAutoAliasFlags[filterCount] = true;
			}

			// look for parameters in the condition.  for each parameter, we:
			//		1) keep track of the name for later
			//		2) // we replace `:{param-name} ` with `:{filter-name}.{param-name} ` in the condition
			final Matcher matcher = FILTER_PARAMETER_PATTERN.matcher( filterConditions[filterCount] );

			String copy = filterConditions[filterCount];
			final List<String> filterParamNames = new ArrayList<>();
			parameterNames[filterCount] = filterParamNames;
			boolean foundAny = false;

			// handle any subsequent matched parameters
			while( matcher.find() ) {
				final String parameterLabel = filterConditions[filterCount].substring( matcher.start() + 1, matcher.end() );
				filterParamNames.add( parameterLabel );
				copy = copy.replace(
						":" + parameterLabel,
						":" + filterName + "." + parameterLabel
				);
				foundAny = true;
			}

			if ( foundAny ) {
				filterConditions[filterCount] = safeInterning( copy );
			}

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

	public static void applyBaseRestrictions(
			Consumer<Predicate> predicateConsumer,
			Restrictable restrictable,
			TableGroup rootTableGroup,
			boolean useIdentificationVariable,
			LoadQueryInfluencers loadQueryInfluencers,
			SqlAstCreationState astCreationState) {
		restrictable.applyBaseRestrictions(
				predicateConsumer,
				rootTableGroup,
				useIdentificationVariable,
				loadQueryInfluencers.getEnabledFilters(),
				null,
				astCreationState
		);
	}

	public void applyEnabledFilters(
			Consumer<Predicate> predicateConsumer,
			FilterAliasGenerator aliasGenerator,
			Map<String, Filter> enabledFilters) {
		final FilterPredicate predicate = generateFilterPredicate( aliasGenerator, enabledFilters );
		if ( predicate != null ) {
			predicateConsumer.accept( predicate );
		}
	}

	private FilterPredicate generateFilterPredicate(FilterAliasGenerator aliasGenerator, Map<String, Filter> enabledFilters) {
		final FilterPredicate filterPredicate = new FilterPredicate();

		for ( int i = 0, max = filterNames.length; i < max; i++ ) {
			final String filterName = filterNames[i];
			final FilterImpl enabledFilter = (FilterImpl) enabledFilters.get( filterName );
			if ( enabledFilter != null ) {
				filterPredicate.applyFragment( render( aliasGenerator, i ), enabledFilter, parameterNames[i] );
			}
		}

		if ( filterPredicate.isEmpty() ) {
			return null;
		}

		return filterPredicate;
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

}
