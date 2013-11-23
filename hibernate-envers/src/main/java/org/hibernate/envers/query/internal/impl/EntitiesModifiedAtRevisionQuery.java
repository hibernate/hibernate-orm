package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * In comparison to {@link EntitiesAtRevisionQuery} this query returns an empty collection if an entity
 * of a certain type has not been changed in a given revision.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @see EntitiesAtRevisionQuery
 */
public class EntitiesModifiedAtRevisionQuery extends AbstractAuditQuery {
	private final Number revision;

	public EntitiesModifiedAtRevisionQuery(
			AuditConfiguration verCfg, AuditReaderImplementor versionsReader,
			Class<?> cls, Number revision) {
		super( verCfg, versionsReader, cls );
		this.revision = revision;
	}

	public EntitiesModifiedAtRevisionQuery(
			AuditConfiguration verCfg, AuditReaderImplementor versionsReader,
			Class<?> cls, String entityName, Number revision) {
		super( verCfg, versionsReader, cls, entityName );
		this.revision = revision;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List list() {
		/*
         * The query that we need to create:
         *   SELECT new list(e) FROM versionsReferencedEntity e
         *   WHERE
         * (all specified conditions, transformed, on the "e" entity) AND
         * e.revision = :revision
         */
		AuditEntitiesConfiguration verEntCfg = verCfg.getAuditEntCfg();
		String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
		qb.getRootParameters().addWhereWithParam( revisionPropertyPath, "=", revision );

		// all specified conditions
		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery( verCfg, versionsReader, entityName, qb, qb.getRootParameters() );
		}

		Query query = buildQuery();
		List queryResult = query.list();

		if ( hasProjection ) {
			return queryResult;
		}
		else {
			List result = new ArrayList();
			entityInstantiator.addInstancesFromVersionsEntities( entityName, result, queryResult, revision );

			return result;
		}
	}
}
