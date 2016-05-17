/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.query;

import java.util.Collections;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.mapper.id.QueryParameterData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.query.Query;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.DEL_REVISION_TYPE_PARAMETER;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Base class for implementers of {@code RelationQueryGenerator} contract.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractRelationQueryGenerator implements RelationQueryGenerator {
	protected final AuditEntitiesConfiguration verEntCfg;
	protected final MiddleIdData referencingIdData;
	protected final boolean revisionTypeInId;

	protected AbstractRelationQueryGenerator(
			AuditEntitiesConfiguration verEntCfg, MiddleIdData referencingIdData,
			boolean revisionTypeInId) {
		this.verEntCfg = verEntCfg;
		this.referencingIdData = referencingIdData;
		this.revisionTypeInId = revisionTypeInId;
	}

	/**
	 * @return Query used to retrieve state of audited entity valid at a given revision.
	 */
	protected abstract String getQueryString();

	/**
	 * @return Query executed to retrieve state of audited entity valid at previous revision
	 *         or removed during exactly specified revision number. Used only when traversing deleted
	 *         entities graph.
	 */
	protected abstract String getQueryRemovedString();

	@Override
	public Query getQuery(AuditReaderImplementor versionsReader, Object primaryKey, Number revision, boolean removed) {
		final Query query = versionsReader.getSession().createQuery( removed ? getQueryRemovedString() : getQueryString() );
		query.setParameter( DEL_REVISION_TYPE_PARAMETER, RevisionType.DEL );
		query.setParameter( REVISION_PARAMETER, revision );
		for ( QueryParameterData paramData : referencingIdData.getPrefixedMapper().mapToQueryParametersFromId(
				primaryKey
		) ) {
			paramData.setParameterValue( query );
		}
		return query;
	}

	protected String getRevisionTypePath() {
		return revisionTypeInId
				? verEntCfg.getOriginalIdPropName() + "." + verEntCfg.getRevisionTypePropName()
				: verEntCfg.getRevisionTypePropName();
	}

	protected String queryToString(QueryBuilder query) {
		return queryToString( query, Collections.emptyMap() );
	}

	protected String queryToString(QueryBuilder query, Map<String, Object> queryParamValues) {
		final StringBuilder sb = new StringBuilder();
		query.build( sb, queryParamValues );
		return sb.toString();
	}
}
