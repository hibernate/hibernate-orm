/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.JoinType;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Chris Cranford
 */
public class RevisionsOfEntityQuery extends AbstractAuditQuery {
	private final boolean selectEntitiesOnly;
	private final boolean selectDeletedEntities;
	private final boolean selectRevisionInfoOnly;

	public RevisionsOfEntityQuery(
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			boolean selectEntitiesOnly,
			boolean selectDeletedEntities,
			boolean selectRevisionInfoOnly) {
		super( versionsReader, cls );

		this.selectEntitiesOnly = selectEntitiesOnly;
		this.selectDeletedEntities = selectDeletedEntities;
		this.selectRevisionInfoOnly = selectRevisionInfoOnly && !selectEntitiesOnly;
	}

	public RevisionsOfEntityQuery(
			AuditReaderImplementor versionsReader,
			Class<?> cls, String entityName,
			boolean selectEntitiesOnly,
			boolean selectDeletedEntities,
			boolean selectRevisionInfoOnly) {
		super( versionsReader, cls, entityName );

		this.selectEntitiesOnly = selectEntitiesOnly;
		this.selectDeletedEntities = selectDeletedEntities;
		this.selectRevisionInfoOnly = selectRevisionInfoOnly && !selectEntitiesOnly;
	}

	private Number getRevisionNumber(Map versionsEntity) {
		final String originalId = versionsReader.getAuditService().getOptions().getOriginalIdPropName();
		String revisionPropertyName = versionsReader.getAuditService().getOptions().getRevisionFieldName();

		Object revisionInfoObject = ( (Map) versionsEntity.get( originalId ) ).get( revisionPropertyName );

		if ( revisionInfoObject instanceof HibernateProxy ) {
			return (Number) ( (HibernateProxy) revisionInfoObject ).getHibernateLazyInitializer().getIdentifier();
		}
		else {
			// Not a proxy - must be read from cache or with a join
			return versionsReader.getAuditService().getRevisionInfoNumberReader().getRevisionNumber( revisionInfoObject );
		}
	}

	@SuppressWarnings({"unchecked"})
	public List list() throws AuditException {
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
			qb.getRootParameters().addWhereWithParam(
					versionsReader.getAuditService().getOptions().getRevisionTypePropName(),
					"<>",
					RevisionType.DEL
			);
		}

		// all specified conditions, transformed
		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery(
					versionsReader,
					aliasToEntityNameMap,
					QueryConstants.REFERENCED_ENTITY_ALIAS,
					qb,
					qb.getRootParameters()
			);
		}

		if ( !hasProjection() && !hasOrder ) {
			String revisionPropertyPath = versionsReader.getAuditService().getOptions().getRevisionNumberPath();
			qb.addOrder( QueryConstants.REFERENCED_ENTITY_ALIAS, revisionPropertyPath, true );
		}

		if ( !selectEntitiesOnly ) {
			qb.addFrom( versionsReader.getAuditService().getOptions().getRevisionInfoEntityName(), "r", true );
			qb.getRootParameters().addWhere(
					versionsReader.getAuditService().getOptions().getRevisionNumberPath(),
					true,
					"=",
					"r.id",
					false
			);
		}

		List<Object> queryResult = buildAndExecuteQuery();
		if ( hasProjection() ) {
			return queryResult;
		}
		else if ( selectRevisionInfoOnly ) {
			return queryResult.stream().map( e -> ( (Object[]) e )[1] ).collect( Collectors.toList() );
		}
		else {
			List entities = new ArrayList();
			String revisionTypePropertyName = versionsReader.getAuditService().getOptions().getRevisionTypePropName();

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

	@Override
	public AuditAssociationQuery<? extends AuditQuery> traverseRelation(String associationName, JoinType joinType) {
		throw new UnsupportedOperationException( "Not yet implemented for revisions of entity queries" );
	}

}
