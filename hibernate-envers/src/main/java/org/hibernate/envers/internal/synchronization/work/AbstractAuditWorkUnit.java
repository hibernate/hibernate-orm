/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.internal.synchronization.work;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.strategy.AuditStrategy;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Stephanie Pau at Markit Group Plc
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractAuditWorkUnit implements AuditWorkUnit {
	protected final SessionImplementor sessionImplementor;
	protected final AuditConfiguration verCfg;
	protected final Serializable id;
	protected final String entityName;
	protected final AuditStrategy auditStrategy;
	protected final RevisionType revisionType;

	private Object performedData;

	protected AbstractAuditWorkUnit(
			SessionImplementor sessionImplementor, String entityName, AuditConfiguration verCfg,
			Serializable id, RevisionType revisionType) {
		this.sessionImplementor = sessionImplementor;
		this.verCfg = verCfg;
		this.id = id;
		this.entityName = entityName;
		this.revisionType = revisionType;
		this.auditStrategy = verCfg.getAuditStrategy();
	}

	protected void fillDataWithId(Map<String, Object> data, Object revision) {
		final AuditEntitiesConfiguration entitiesCfg = verCfg.getAuditEntCfg();

		final Map<String, Object> originalId = new HashMap<String, Object>();
		originalId.put( entitiesCfg.getRevisionFieldName(), revision );

		verCfg.getEntCfg().get( getEntityName() ).getIdMapper().mapToMapFromId( originalId, id );
		data.put( entitiesCfg.getRevisionTypePropName(), revisionType );
		data.put( entitiesCfg.getOriginalIdPropName(), originalId );
	}

	@Override
	public void perform(Session session, Object revisionData) {
		final Map<String, Object> data = generateData( revisionData );

		auditStrategy.perform( session, getEntityName(), verCfg, id, data, revisionData );

		setPerformed( data );
	}

	@Override
	public Serializable getEntityId() {
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
			session.delete( verCfg.getAuditEntCfg().getAuditEntityName( getEntityName() ), performedData );
			session.flush();
		}
	}

	@Override
	public RevisionType getRevisionType() {
		return revisionType;
	}
}
