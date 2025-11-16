/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.filter.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.hibernate.Filter;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.metamodel.mapping.Restrictable;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;

import static org.hibernate.internal.FilterImpl.MARKER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.replace;
import static org.hibernate.internal.util.StringHelper.safeInterning;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * Utility methods for dealing with {@linkplain FilterConfiguration filters}.
 *
 * @author Steve Ebersole
 * @author Rob Worsnop
 * @author Nathan Xu
 */
public class FilterHelper {
	private static final Pattern FILTER_PARAMETER_PATTERN = Pattern.compile( ":((\\S+)(\\w+))" );

	private final String[] filterNames;
	private final String[] filterConditions;
	private final boolean[] filterAutoAliasFlags;
	private final Map<String, String>[] filterAliasTableMaps;
	private final List<String>[] parameterNames;
	private final Map<String, String> tableToEntityName;

	public FilterHelper(List<FilterConfiguration> filters, SessionFactoryImplementor factory) {
		this( filters, null, factory );
	}

	/**
	 * The map of defined filters.  This is expected to be in format
	 * where the filter names are the map keys, and the defined
	 * conditions are the values.
	 *
	 * @param filters The map of defined filters.
	 * @param factory The session factory
	 */
	public FilterHelper(List<FilterConfiguration> filters, Map<String, String> tableToEntityName, SessionFactoryImplementor factory) {
		int filterCount = filters.size();

		filterNames = new String[filterCount];
		filterConditions = new String[filterCount];
		filterAutoAliasFlags = new boolean[filterCount];
		filterAliasTableMaps = new Map[filterCount];
		parameterNames = new List[filterCount];
		this.tableToEntityName = tableToEntityName;

		filterCount = 0;
		for ( final FilterConfiguration filter : filters ) {
			final String filterName = safeInterning( filter.getName() );
			filterNames[filterCount] = filterName;
			filterConditions[filterCount] = safeInterning( filter.getCondition() );

			filterAliasTableMaps[filterCount] = filter.getAliasTableMap( factory );
			filterAutoAliasFlags[filterCount] = false;

			injectAliases( factory, filter, filterCount );
			qualifyParameterNames( filterCount, filterName );

			filterCount++;
		}
	}

	private void injectAliases(SessionFactoryImplementor factory, FilterConfiguration filter, int filterCount) {
		if ( ( filterAliasTableMaps[filterCount].isEmpty()
				|| isTableFromPersistentClass( filterAliasTableMaps[filterCount] ) )
				&& filter.useAutoAliasInjection() ) {
			final String autoAliasedCondition = Template.renderWhereStringTemplate(
					filter.getCondition(),
					MARKER,
					factory.getJdbcServices().getDialect(),
					factory.getTypeConfiguration()
			);
			filterConditions[filterCount] = safeInterning( autoAliasedCondition );
			filterAutoAliasFlags[filterCount] = true;
		}
	}

	/**
	 * Look for parameters in the given condition. For each parameter, we:
	 * <ol>
	 *     <li>keep track of the name for later</li>
	 *     <li>replace {@code :{param-name}} with {@code :{filter-name}.{param-name}}
	 *     in the condition</li>
	 * </ol>
	 */
	private void qualifyParameterNames(int filterCount, String filterName) {
		final List<String> parameterNames = new ArrayList<>();
		boolean foundAny = false;
		final var matcher = FILTER_PARAMETER_PATTERN.matcher( filterConditions[filterCount] );
		while ( matcher.find() ) {
			parameterNames.add( matcher.group(1) );
			foundAny = true;
		}
		if ( foundAny ) {
			filterConditions[filterCount] =
					safeInterning( matcher.replaceAll(":" + filterName +  ".$1") );
		}
		this.parameterNames[filterCount] = parameterNames;
	}

	private static boolean isTableFromPersistentClass(Map<String, String> aliasTableMap) {
		return aliasTableMap.size() == 1 && aliasTableMap.containsKey( null );
	}

	public String[] getFilterNames() {
		return filterNames;
	}

	public boolean isAffectedBy(Map<String, Filter> enabledFilters) {
		return isAffectedBy( enabledFilters, false );
	}

	public boolean isAffectedBy(Map<String, Filter> enabledFilters, boolean onlyApplyForLoadByKey) {
		for ( String filterName : filterNames ) {
			final var filter = enabledFilters.get( filterName );
			if ( filter != null
					&& ( !onlyApplyForLoadByKey || filter.isAppliedToLoadByKey() ) ) {
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
				astCreationState.applyOnlyLoadByKeyFilters(),
				null,
				astCreationState
		);
	}

	public void applyEnabledFilters(
			Consumer<Predicate> predicateConsumer,
			FilterAliasGenerator aliasGenerator,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		final var predicate = generateFilterPredicate(
				aliasGenerator,
				enabledFilters,
				onlyApplyLoadByKeyFilters,
				tableGroup,
				creationState
		);
		if ( predicate != null ) {
			predicateConsumer.accept( predicate );
		}
	}

	private FilterPredicate generateFilterPredicate(
			FilterAliasGenerator aliasGenerator,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		final var filterPredicate = new FilterPredicate();

		for ( int i = 0, max = filterNames.length; i < max; i++ ) {
			final var enabledFilter = enabledFilters.get( filterNames[i] );
			if ( enabledFilter != null && ( !onlyApplyLoadByKeyFilters || enabledFilter.isAppliedToLoadByKey() ) ) {
				filterPredicate.applyFragment( render( aliasGenerator, i, tableGroup, creationState ), enabledFilter, parameterNames[i] );
			}
		}

		if ( filterPredicate.isEmpty() ) {
			return null;
		}

		return filterPredicate;
	}

	public String render(FilterAliasGenerator aliasGenerator, Map<String, Filter> enabledFilters) {
		final var buffer = new StringBuilder();
		render( buffer, aliasGenerator, enabledFilters );
		return buffer.toString();
	}

	public void render(StringBuilder buffer, FilterAliasGenerator aliasGenerator, Map<String, Filter> enabledFilters) {
		if ( isNotEmpty( filterNames ) ) {
			for ( int i = 0, max = filterNames.length; i < max; i++ ) {
				if ( enabledFilters.containsKey( filterNames[i] ) ) {
					if ( isNotEmpty( filterConditions[i] ) ) {
						if ( !buffer.isEmpty() ) {
							buffer.append( " and " );
						}
						buffer.append( render( aliasGenerator, i, null, null ) );
					}
				}
			}
		}
	}

	private String render(
			FilterAliasGenerator aliasGenerator,
			int filterIndex,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		final String condition = filterConditions[filterIndex];
		if ( aliasGenerator == null ) {
			return replace( condition, MARKER + ".", "");
		}
		else {
			final var aliasTableMap = filterAliasTableMaps[filterIndex];
			if ( filterAutoAliasFlags[filterIndex] ) {
				final String tableName = aliasTableMap.get( null );
				return replaceMarker( tableGroup, creationState, condition,
						aliasGenerator.getAlias( tableName ),
						tableName( tableGroup, tableName ) );
			}
			else if ( isTableFromPersistentClass( aliasTableMap ) ) {
				final String tableName = aliasTableMap.get( null );
				return replaceAlias( tableGroup, creationState, condition,
						"{alias}",
						aliasGenerator.getAlias( tableName ),
						tableName( tableGroup, tableName ) );
			}
			else {
				String newCondition = condition;
				for ( var entry : aliasTableMap.entrySet() ) {
					final String tableName = entry.getValue();
					newCondition =
							replaceAlias( tableGroup, creationState, newCondition,
									"{" + entry.getKey() + "}",
									aliasGenerator.getAlias( tableName ),
									tableName );
				}
				return newCondition;
			}
		}
	}

	private String replaceMarker(
			TableGroup tableGroup, SqlAstCreationState creationState,
			String condition, String alias, String tableName) {
		final String newCondition = replace( condition, MARKER, alias );
		if ( creationState != null
				&& tableToEntityName != null
				&& !newCondition.equals(condition) ) {
			registerEntityNameUsage( tableGroup, creationState, tableName );
		}
		return newCondition;
	}

	private String replaceAlias(
			TableGroup tableGroup, SqlAstCreationState creationState,
			String condition, String placeholder, String alias, String tableName) {
		final String newCondition = replace( condition, placeholder, alias );
		if ( creationState != null
				&& !newCondition.equals(condition) ) {
			registerEntityNameUsage( tableGroup, creationState, tableName );
		}
		return newCondition;
	}

	private void registerEntityNameUsage(TableGroup tableGroup, SqlAstCreationState creationState, String tableName) {
		creationState.registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
				tableToEntityName.get( tableName ) );
	}

	private static String tableName(TableGroup tableGroup, String tableName) {
		return tableName == null
				? tableGroup.getPrimaryTableReference().getTableId()
				: tableName;
	}
}
