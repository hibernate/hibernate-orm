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
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 */
public class RevisionsOfEntityQuery extends AbstractAuditQuery {
	private final boolean selectEntitiesOnly;
	private final boolean selectDeletedEntities;

	public RevisionsOfEntityQuery(
			AuditConfiguration verCfg,
			AuditReaderImplementor versionsReader,
			Class<?> cls, boolean selectEntitiesOnly,
			boolean selectDeletedEntities) {
		super( verCfg, versionsReader, cls );

		this.selectEntitiesOnly = selectEntitiesOnly;
		this.selectDeletedEntities = selectDeletedEntities;
	}

	public RevisionsOfEntityQuery(
			AuditConfiguration verCfg,
			AuditReaderImplementor versionsReader, Class<?> cls, String entityName,
			boolean selectEntitiesOnly, boolean selectDeletedEntities) {
		super( verCfg, versionsReader, cls, entityName );

		this.selectEntitiesOnly = selectEntitiesOnly;
		this.selectDeletedEntities = selectDeletedEntities;
	}

	private Number getRevisionNumber(Map versionsEntity) {
		AuditEntitiesConfiguration verEntCfg = verCfg.getAuditEntCfg();

		String originalId = verEntCfg.getOriginalIdPropName();
		String revisionPropertyName = verEntCfg.getRevisionFieldName();

		Object revisionInfoObject = ((Map) versionsEntity.get( originalId )).get( revisionPropertyName );

		if ( revisionInfoObject instanceof HibernateProxy ) {
			return (Number) ((HibernateProxy) revisionInfoObject).getHibernateLazyInitializer().getIdentifier();
		}
		else {
			// Not a proxy - must be read from cache or with a join
			return verCfg.getRevisionInfoNumberReader().getRevisionNumber( revisionInfoObject );
		}
	}

	@SuppressWarnings({"unchecked"})
	public List list() throws AuditException {
		AuditEntitiesConfiguration verEntCfg = verCfg.getAuditEntCfg();

        /*
		The query that should be executed in the versions table:
        SELECT e (unless another projection is specified) FROM ent_ver e, rev_entity r WHERE
          e.revision_type != DEL (if selectDeletedEntities == false) AND
          e.revision = r.revision AND
          (all specified conditions, transformed, on the "e" entity)
          ORDER BY e.revision ASC (unless another order or projection is specified)
         */
		if ( !selectDeletedEntities ) {
			// e.revision_type != DEL AND
			qb.getRootParameters().addWhereWithParam( verEntCfg.getRevisionTypePropName(), "<>", RevisionType.DEL );
		}

		// all specified conditions, transformed
		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery( verCfg, versionsReader, entityName, qb, qb.getRootParameters() );
		}

		if ( !hasProjection && !hasOrder ) {
			String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
			qb.addOrder( revisionPropertyPath, true );
		}

		if ( !selectEntitiesOnly ) {
			qb.addFrom( verCfg.getAuditEntCfg().getRevisionInfoEntityName(), "r" );
			qb.getRootParameters().addWhere(
					verCfg.getAuditEntCfg().getRevisionNumberPath(),
					true,
					"=",
					"r.id",
					false
			);
		}

		List<Object> queryResult = buildAndExecuteQuery();
		if ( hasProjection ) {
			return queryResult;
		}
		else {
			List entities = new ArrayList();
			String revisionTypePropertyName = verEntCfg.getRevisionTypePropName();

			for ( Object resultRow : queryResult ) {
				Map versionsEntity;
				Object revisionData;

				if ( selectEntitiesOnly ) {
					versionsEntity = (Map) resultRow;
					revisionData = null;
				}
				else {
					Object[] arrayResultRow = (Object[]) resultRow;
					versionsEntity = (Map) arrayResultRow[0];
					revisionData = arrayResultRow[1];
				}

				Number revision = getRevisionNumber( versionsEntity );

				Object entity = entityInstantiator.createInstanceFromVersionsEntity(
						entityName,
						versionsEntity,
						revision
				);

				if ( !selectEntitiesOnly ) {
					entities.add( new Object[] {entity, revisionData, versionsEntity.get( revisionTypePropertyName )} );
				}
				else {
					entities.add( entity );
				}
			}

			return entities;
		}
	}
}
