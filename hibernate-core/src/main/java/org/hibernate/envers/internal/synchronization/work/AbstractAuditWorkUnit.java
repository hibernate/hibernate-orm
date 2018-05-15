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
import org.hibernate.envers.boot.AuditService;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Stephanie Pau at Markit Group Plc
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class AbstractAuditWorkUnit implements AuditWorkUnit {
	protected final SessionImplementor sessionImplementor;
	protected final AuditService auditService;
	protected final Object id;
	protected final String entityName;
	protected final RevisionType revisionType;

	private Object performedData;

	protected AbstractAuditWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			AuditService auditService,
			Object id,
			RevisionType revisionType) {
		this.sessionImplementor = sessionImplementor;
		this.auditService = auditService;
		this.id = id;
		this.entityName = entityName;
		this.revisionType = revisionType;
	}

	protected void fillDataWithId(Map<String, Object> data, Object revision) {

		final Map<String, Object> originalId = new HashMap<>();
		originalId.put( auditService.getOptions().getRevisionFieldName(), revision );

		auditService.getEntityBindings().get( getEntityName() ).getIdMapper().mapToMapFromId( originalId, id );
		data.put( auditService.getOptions().getRevisionTypePropName(), revisionType );
		data.put( auditService.getOptions().getOriginalIdPropName(), originalId );
	}

	@Override
	public void perform(Session session, Object revisionData) {
		final Map<String, Object> data = generateData( revisionData );
		auditService.getOptions().getAuditStrategy().perform(
				session,
				getEntityName(),
				auditService,
				id,
				data,
				revisionData
		);
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
			session.delete( auditService.getAuditEntityName( entityName ), performedData );
			session.flush();
		}
	}

	@Override
	public RevisionType getRevisionType() {
		return revisionType;
	}
}
