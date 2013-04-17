/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.entities.mapper.relation.query;

import java.util.Collections;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.entities.mapper.id.QueryParameterData;
import org.hibernate.envers.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.query.QueryBuilder;

import static org.hibernate.envers.entities.mapper.relation.query.QueryConstants.DEL_REVISION_TYPE_PARAMETER;
import static org.hibernate.envers.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Base class for implementers of {@code RelationQueryGenerator} contract.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractRelationQueryGenerator implements RelationQueryGenerator {
	protected final AuditEntitiesConfiguration verEntCfg;
	protected final MiddleIdData referencingIdData;
	protected final boolean revisionTypeInId;

	protected AbstractRelationQueryGenerator(AuditEntitiesConfiguration verEntCfg, MiddleIdData referencingIdData,
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
	 * or removed during exactly specified revision number. Used only when traversing deleted
	 * entities graph.
	 */
	protected abstract String getQueryRemovedString();

	public Query getQuery(AuditReaderImplementor versionsReader, Object primaryKey, Number revision, boolean removed) {
		Query query = versionsReader.getSession().createQuery( removed ? getQueryRemovedString() : getQueryString() );
		query.setParameter( DEL_REVISION_TYPE_PARAMETER, RevisionType.DEL );
		query.setParameter( REVISION_PARAMETER, revision );
		for ( QueryParameterData paramData : referencingIdData.getPrefixedMapper().mapToQueryParametersFromId( primaryKey ) ) {
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
		return queryToString( query, Collections.<String, Object>emptyMap() );
	}

	protected String queryToString(QueryBuilder query, Map<String, Object> queryParamValues) {
		final StringBuilder sb = new StringBuilder();
		query.build( sb, queryParamValues );
		return sb.toString();
	}
}