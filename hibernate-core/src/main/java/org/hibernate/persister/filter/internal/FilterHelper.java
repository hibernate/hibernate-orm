/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.filter.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.hibernate.Filter;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.FilterConfiguration;
import org.hibernate.metamodel.mapping.Restrictable;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.FilterPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
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
	@Nullable
	private final Map<String, String> tableToEntityName;

	public FilterHelper(@Nonnull List<FilterConfiguration> filters, @Nonnull SessionFactoryImplementor factory) {
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
	public FilterHelper(@Nonnull List<FilterConfiguration> filters, @Nullable Map<String, String> tableToEntityName, @Nonnull SessionFactoryImplementor factory) {
		final int filterCount = filters.size();

		filterNames = new String[filterCount];
		filterConditions = new String[filterCount];
		filterAutoAliasFlags = new boolean[filterCount];
		filterAliasTableMaps = new Map[filterCount];
		parameterNames = new List[filterCount];
		this.tableToEntityName = tableToEntityName;

		for ( int i = 0; i < filters.size(); i++ ) {
			final var filter = filters.get( i );
			final String filterName = safeInterning( filter.getName() );
			filterNames[i] = filterName;
			filterConditions[i] = safeInterning( filter.getCondition() );

			filterAliasTableMaps[i] = filter.getAliasTableMap( factory );
			filterAutoAliasFlags[i] = false;

			injectAliases( factory, filter, i );
			qualifyParameterNames( i, filterName );
		}
	}

	private void injectAliases(@Nonnull SessionFactoryImplementor factory, @Nonnull FilterConfiguration filter, int filterCount) {
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
	private void qualifyParameterNames(int filterCount, @Nonnull String filterName) {
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

	private static boolean isTableFromPersistentClass(@Nonnull Map<String, String> aliasTableMap) {
		return aliasTableMap.size() == 1 && aliasTableMap.containsKey( null );
	}

	@Nonnull
	public String[] getFilterNames() {
		return filterNames;
	}

	public boolean isAffectedBy(@Nonnull Map<String, Filter> enabledFilters) {
		return isAffectedBy( enabledFilters, false );
	}

	public boolean isAffectedBy(@Nonnull Map<String, Filter> enabledFilters, boolean onlyApplyForLoadByKey) {
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
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull Restrictable restrictable,
			@Nonnull TableGroup rootTableGroup,
			boolean useIdentificationVariable,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull SqlAstCreationState astCreationState) {
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
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nullable FilterAliasGenerator aliasGenerator,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable TableGroup tableGroup,
			@Nullable SqlAstCreationState creationState) {
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

	@Nullable
	private FilterPredicate generateFilterPredicate(
			@Nullable FilterAliasGenerator aliasGenerator,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable TableGroup tableGroup,
			@Nullable SqlAstCreationState creationState) {
		final var filterPredicate = new FilterPredicate();

		for ( int i = 0, max = filterNames.length; i < max; i++ ) {
			final var enabledFilter = enabledFilters.get( filterNames[i] );
			if ( enabledFilter != null && ( !onlyApplyLoadByKeyFilters || enabledFilter.isAppliedToLoadByKey() ) ) {
				filterPredicate.applyFragment( render( aliasGenerator, i, tableGroup, creationState ),
						enabledFilter, parameterNames[i] );
			}
		}

		return filterPredicate.isEmpty() ? null : filterPredicate;

	}

	@Nonnull
	public String render(@Nullable FilterAliasGenerator aliasGenerator, @Nonnull Map<String, Filter> enabledFilters) {
		final var buffer = new StringBuilder();
		render( buffer, aliasGenerator, enabledFilters );
		return buffer.toString();
	}

	public void render(@Nonnull StringBuilder buffer, @Nullable FilterAliasGenerator aliasGenerator, @Nonnull Map<String, Filter> enabledFilters) {
		if ( isNotEmpty( filterNames ) ) {
			for ( int i = 0, max = filterNames.length; i < max; i++ ) {
				if ( enabledFilters.containsKey( filterNames[i] )
						&& isNotEmpty( filterConditions[i] ) ) {
					if ( !buffer.isEmpty() ) {
						buffer.append( " and " );
					}
					buffer.append( render( aliasGenerator, i, null, null ) );
				}
			}
		}
	}

	@Nonnull
	private String render(
			@Nullable FilterAliasGenerator aliasGenerator,
			int filterIndex,
			@Nullable TableGroup tableGroup,
			@Nullable SqlAstCreationState creationState) {
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

	@Nonnull
	private String replaceMarker(
			@Nullable TableGroup tableGroup, @Nullable SqlAstCreationState creationState,
			@Nonnull String condition, @Nullable String alias, @Nullable String tableName) {
		final String newCondition = replace( condition, MARKER, alias );
		if ( creationState != null
				&& tableToEntityName != null
				&& !newCondition.equals(condition) ) {
			registerEntityNameUsage( castNonNull( tableGroup ), creationState, tableName );
		}
		return newCondition;
	}

	@Nonnull
	private String replaceAlias(
			@Nullable TableGroup tableGroup, @Nullable SqlAstCreationState creationState,
			@Nonnull String condition, @Nonnull String placeholder, @Nullable String alias, @Nullable String tableName) {
		final String newCondition = replace( condition, placeholder, alias );
		if ( creationState != null
				&& tableToEntityName != null
				&& !newCondition.equals(condition) ) {
			registerEntityNameUsage( castNonNull( tableGroup ), creationState, tableName );
		}
		return newCondition;
	}

	private void registerEntityNameUsage(@Nonnull TableGroup tableGroup, @Nonnull SqlAstCreationState creationState, @Nullable String tableName) {
		String treatTargetTypeName = castNonNull( tableToEntityName ).get( tableName );
		if (treatTargetTypeName != null) {
			creationState.registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
					treatTargetTypeName );
		}
	}

	@Nullable
	private static String tableName(@Nullable TableGroup tableGroup, @Nullable String tableName) {
		return tableName == null && tableGroup != null
				? tableGroup.getPrimaryTableReference().getTableId()
				: tableName;
	}
}
