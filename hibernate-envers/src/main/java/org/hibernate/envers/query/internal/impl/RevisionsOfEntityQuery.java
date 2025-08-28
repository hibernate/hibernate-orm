/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.impl;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.ExtendedPropertyMapper;
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
	private final boolean includePropertyChanges;

	public RevisionsOfEntityQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			boolean selectEntitiesOnly,
			boolean selectDeletedEntities,
			boolean selectRevisionInfoOnly,
			boolean includePropertyChanges) {
		super( enversService, versionsReader, cls );

		this.selectEntitiesOnly = selectEntitiesOnly;
		this.selectDeletedEntities = selectDeletedEntities;
		this.selectRevisionInfoOnly = selectRevisionInfoOnly && !selectEntitiesOnly;
		this.includePropertyChanges = includePropertyChanges;
	}

	public RevisionsOfEntityQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls, String entityName,
			boolean selectEntitiesOnly,
			boolean selectDeletedEntities,
			boolean selectRevisionInfoOnly,
			boolean includePropertyChanges) {
		super( enversService, versionsReader, cls, entityName );

		this.selectEntitiesOnly = selectEntitiesOnly;
		this.selectDeletedEntities = selectDeletedEntities;
		this.selectRevisionInfoOnly = selectRevisionInfoOnly && !selectEntitiesOnly;
		this.includePropertyChanges = includePropertyChanges;
	}

	@Override
	public AuditAssociationQuery<? extends AuditQuery> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias,
			AuditCriterion onClauseCriterion) {
		if ( !selectEntitiesOnly ) {
			throw new IllegalStateException(
					"Audit association queries are only permitted when the query is created with selectEntitiesOnly=true"
			);
		}

		AbstractAuditAssociationQuery<AuditQueryImplementor> query = associationQueryMap.get( associationName );
		if ( query == null ) {
			query = new RevisionsOfEntityAssociationQuery<>(
					enversService,
					versionsReader,
					this,
					qb,
					associationName,
					joinType,
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					REFERENCED_ENTITY_ALIAS,
					alias,
					null
			);

			addAssociationQuery( associationName, query );
		}

		return query;
	}

	private Number getRevisionNumber(Map versionsEntity) {
		Configuration configuration = enversService.getConfig();

		String originalId = configuration.getOriginalIdPropertyName();
		String revisionPropertyName = configuration.getRevisionFieldName();

		Object revisionInfoObject = ( (Map) versionsEntity.get( originalId ) ).get( revisionPropertyName );

		if ( revisionInfoObject instanceof HibernateProxy ) {
			return (Number) ( (HibernateProxy) revisionInfoObject ).getHibernateLazyInitializer().getInternalIdentifier();
		}
		else {
			// Not a proxy - must be read from cache or with a join
			return enversService.getRevisionInfoNumberReader().getRevisionNumber( revisionInfoObject );
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public List list() throws AuditException {
		Configuration configuration = enversService.getConfig();

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
			qb.getRootParameters().addWhereWithParam( configuration.getRevisionTypePropertyName(), "<>", RevisionType.DEL );
		}

		// all specified conditions, transformed
		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery(
					enversService,
					versionsReader,
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					QueryConstants.REFERENCED_ENTITY_ALIAS,
					qb,
					qb.getRootParameters()
			);
		}

		for ( AbstractAuditAssociationQuery<?> associationQuery : associationQueries ) {
			associationQuery.addCriterionToQuery( versionsReader );
		}

		if ( !hasProjection() && !hasOrder ) {
			String revisionPropertyPath = configuration.getRevisionNumberPath();
			qb.addOrder( QueryConstants.REFERENCED_ENTITY_ALIAS, revisionPropertyPath, true, null );
		}

		if ( !selectEntitiesOnly ) {
			qb.addFrom( configuration.getRevisionInfo().getRevisionInfoEntityName(), "r", true );
			qb.getRootParameters().addWhere(
					configuration.getRevisionNumberPath(),
					true,
					"=",
					"r.id",
					false
			);
		}

		return getQueryResults();
	}

	private boolean isEntityUsingModifiedFlags() {
		// todo: merge HHH-8973 ModifiedFlagMapperSupport into 6.0 to get this behavior by default
		final ExtendedPropertyMapper propertyMapper = getEntityConfiguration().getPropertyMapper();
		for ( PropertyData propertyData : propertyMapper.getProperties().keySet() ) {
			if ( propertyData.isUsingModifiedFlag() ) {
				return true;
			}
		}
		return false;
	}

	private Set<String> getChangedPropertyNames(Map<String, Object> dataMap, Object revisionType) {
		final Set<String> changedPropertyNames = new HashSet<>();
		// we're only interested in changed properties on modification rows.
		if ( revisionType == RevisionType.MOD ) {
			final String modifiedFlagSuffix = enversService.getConfig().getModifiedFlagsSuffix();
			for ( Map.Entry<String, Object> entry : dataMap.entrySet() ) {
				final String key = entry.getKey();
				if  ( key.endsWith( modifiedFlagSuffix ) ) {
					if ( entry.getValue() != null && Boolean.parseBoolean( entry.getValue().toString() ) ) {
						changedPropertyNames.add( key.substring( 0, key.length() - modifiedFlagSuffix.length() ) );
					}
				}
			}
		}
		return changedPropertyNames;
	}

	private List getQueryResults() {
		List<?> queryResults = buildAndExecuteQuery();
		if ( hasProjection() ) {
			return queryResults;
		}
		else if ( selectRevisionInfoOnly ) {
			return queryResults.stream().map( e -> ( (Object[]) e )[1] ).collect( Collectors.toList() );
		}
		else {
			List entities = new ArrayList();
			if ( selectEntitiesOnly ) {
				for ( Object row : queryResults ) {
					final Map versionsEntity = (Map) row;
					entities.add( getQueryResultRowValue( versionsEntity, null, getEntityName() ) );
				}
			}
			else {
				for ( Object row : queryResults ) {
					final Object[] rowArray = (Object[]) row;
					final Map versionsEntity = (Map) rowArray[ 0 ];
					final Object revisionData = rowArray[ 1 ];
					entities.add( getQueryResultRowValue( versionsEntity, revisionData, getEntityName() ) );
				}
			}
			return entities;
		}
	}

	private Object getQueryResultRowValue(Map versionsData, Object revisionData, String entityName) {
		final Number revision = getRevisionNumber( versionsData );

		final Object entity = entityInstantiator.createInstanceFromVersionsEntity( entityName, versionsData, revision );
		if ( selectEntitiesOnly ) {
			return entity;
		}

		final String revisionTypePropertyName = enversService.getConfig().getRevisionTypePropertyName();
		Object revisionType = versionsData.get( revisionTypePropertyName );
		if ( !includePropertyChanges ) {
			return new Object[] { entity, revisionData, revisionType };
		}

		if ( !isEntityUsingModifiedFlags() ) {
			throw new AuditException(
					String.format(
							Locale.ROOT,
							"The specified entity [%s] does not support or use modified flags.",
							getEntityConfiguration().getEntityClassName()
					)
			);
		}

		final Set<String> changedPropertyNames =  getChangedPropertyNames( versionsData, revisionType );
		return new Object[] { entity, revisionData, revisionType, changedPropertyNames };
	}
}
