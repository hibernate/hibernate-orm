/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.synchronization.work;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.strategy.AuditStrategy;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Stephanie Pau at Markit Group Plc
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class AbstractAuditWorkUnit implements AuditWorkUnit {
	protected final SessionImplementor sessionImplementor;
	protected final EnversService enversService;
	protected final Object id;
	protected final String entityName;
	protected final AuditStrategy auditStrategy;
	protected final RevisionType revisionType;

	private Object performedData;

	protected AbstractAuditWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			EnversService enversService,
			Object id,
			RevisionType revisionType) {
		this.sessionImplementor = sessionImplementor;
		this.enversService = enversService;
		this.id = id;
		this.entityName = entityName;
		this.revisionType = revisionType;
		this.auditStrategy = enversService.getAuditStrategy();
	}

	protected void fillDataWithId(Map<String, Object> data, Object revision) {
		final Configuration configuration = enversService.getConfig();

		final Map<String, Object> originalId = new HashMap<>();
		originalId.put( configuration.getRevisionFieldName(), revision );

		final IdMapper idMapper = enversService.getEntitiesConfigurations().get( getEntityName() ).getIdMapper();
		idMapper.mapToMapFromId( sessionImplementor, originalId, id );

		data.put( configuration.getRevisionTypePropertyName(), revisionType );
		data.put( configuration.getOriginalIdPropertyName(), originalId );
		// The $type$ property holds the name of the (versions) entity
		data.put( "$type$", configuration.getAuditEntityName( entityName ) );
	}

	@Override
	public void perform(Session session, Object revisionData) {
		final Map<String, Object> data = generateData( revisionData );

		auditStrategy.perform( session, getEntityName(), enversService, id, data, revisionData );

		setPerformed( data );
	}

	@Override
	public Object getEntityId() {
		return id;
	}

	@Override
	public boolean isPerformed() {
		return performedData != null;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	protected void setPerformed(Object performedData) {
		this.performedData = performedData;
	}

	public void undo(Session session) {
		if ( isPerformed() ) {
			session.delete(
					enversService.getConfig().getAuditEntityName( getEntityName() ),
					performedData
			);
			session.flush();
		}
	}

	@Override
	public RevisionType getRevisionType() {
		return revisionType;
	}
}
