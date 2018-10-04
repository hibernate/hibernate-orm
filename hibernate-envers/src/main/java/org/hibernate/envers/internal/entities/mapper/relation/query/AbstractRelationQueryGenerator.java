/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.query;

import java.util.Collections;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.id.QueryParameterData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.query.Query;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.DEL_REVISION_TYPE_PARAMETER;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Base class for implementers of {@code RelationQueryGenerator} contract.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class AbstractRelationQueryGenerator implements RelationQueryGenerator {
	protected final GlobalConfiguration globalCfg;
	protected final AuditEntitiesConfiguration verEntCfg;
	protected final AuditStrategy auditStrategy;
	protected final MiddleIdData referencingIdData;
	protected final boolean revisionTypeInId;
	protected final String entityName;
	protected final String orderBy;

	private String queryString;
	private String queryRemovedString;

	protected AbstractRelationQueryGenerator(
			GlobalConfiguration globalCfg,
			AuditEntitiesConfiguration verEntCfg,
			AuditStrategy auditStrategy,
			String entityName,
			MiddleIdData referencingIdData,
			boolean revisionTypeInId,
			String orderBy) {
		this.globalCfg = globalCfg;
		this.verEntCfg = verEntCfg;
		this.entityName = entityName;
		this.auditStrategy = auditStrategy;
		this.referencingIdData = referencingIdData;
		this.revisionTypeInId = revisionTypeInId;
		this.orderBy = orderBy;
	}

	@Override
	public Query getQuery(SharedSessionContractImplementor session, Object primaryKey, Number revision, boolean removed) {
		final String queryString = getQueryString( session.getFactory(), removed );

		final Query query = session.createQuery( queryString );
		query.setParameter( DEL_REVISION_TYPE_PARAMETER, RevisionType.DEL );
		query.setParameter( REVISION_PARAMETER, revision );

		final IdMapper prefixIdMapper = referencingIdData.getPrefixedMapper();
		for ( QueryParameterData paramData : prefixIdMapper.mapToQueryParametersFromId( primaryKey ) ) {
			paramData.setParameterValue( query );
		}

		return query;
	}

	/**
	 * Build the common aspects of a {@link QueryBuilder} used by both query and query-remove strings.
	 *
	 * @param sessionFactory The session factory.
	 * @return The constructed query builder instance.
	 */
	protected abstract QueryBuilder buildQueryBuilderCommon(SessionFactoryImplementor sessionFactory);

	/**
	 * Apply predicates used to fetch actual data.
	 *
	 * @param qb The query builder instance to apply predicates against.
	 * @param parameters The root query parameters
	 * @param inclusive Whether its inclusive or not.
	 */
	protected abstract void applyValidPredicates(QueryBuilder qb, Parameters parameters, boolean inclusive);

	/**
	 * Apply predicates to fetch data and deletions that took place during the same revision.
	 *
	 * @param qb The query builder instance to apply predicates against.
	 */
	protected abstract void applyValidAndRemovePredicates(QueryBuilder qb);

	protected String getRevisionTypePath() {
		if ( revisionTypeInId ) {
			return verEntCfg.getOriginalIdPropName() + "." + verEntCfg.getRevisionTypePropName();
		}
		return verEntCfg.getRevisionTypePropName();
	}

	/**
	 * Get the query to be used.
	 *
	 * If {@code removed} is specified as {@code true}, the removal query will be returned.
	 * If {@code removed} is specified as {@code false}, the non-removal query will be returned.
	 *
	 * This method internally will cache the built queries so that subsequent calls will not
	 * require the rebuilding of the queries.
	 *
	 * @param sessionFactory The session factory.
	 * @param removed Whether to return the removal query or non-removal query.
	 * @return The query string to be used.
	 */
	private String getQueryString(SessionFactoryImplementor sessionFactory, boolean removed) {
		if ( removed ) {
			if ( queryRemovedString == null ) {
				queryRemovedString = buildQueryRemoveString( sessionFactory );
			}
			return queryRemovedString;
		}

		if ( queryString == null ) {
			queryString = buildQueryString( sessionFactory );
		}
		return queryString;
	}

	private String buildQueryString(SessionFactoryImplementor sessionFactory) {
		final QueryBuilder builder = buildQueryBuilderCommon( sessionFactory );
		applyValidPredicates( builder, builder.getRootParameters(), true );
		return queryToString( builder );
	}

	private String buildQueryRemoveString(SessionFactoryImplementor sessionFactory) {
		final QueryBuilder builder = buildQueryBuilderCommon( sessionFactory );
		applyValidAndRemovePredicates( builder );
		return queryToString( builder );
	}

	private String queryToString(QueryBuilder query) {
		return queryToString( query, Collections.emptyMap() );
	}

	private String queryToString(QueryBuilder query, Map<String, Object> queryParamValues) {
		final StringBuilder sb = new StringBuilder();
		query.build( sb, queryParamValues );
		return sb.toString();
	}
}
