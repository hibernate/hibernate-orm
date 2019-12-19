/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.revisioninfo;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class DefaultRevisionInfoGenerator implements RevisionInfoGenerator {
	private final String revisionInfoEntityName;
	private final ManagedBean<? extends RevisionListener> listenerManagedBean;
	private final Setter revisionTimestampSetter;
	private final boolean timestampAsDate;
	private final Constructor<?> revisionInfoClassConstructor;
	private final SessionCacheCleaner sessionCacheCleaner;

	private RevisionInfoNumberReader revisionInfoNumberReader;

	public DefaultRevisionInfoGenerator(
			String revisionInfoEntityName,
			Class<?> revisionInfoClass,
			Class<? extends RevisionListener> listenerClass,
			PropertyData revisionInfoTimestampData,
			boolean timestampAsDate,
			ServiceRegistry serviceRegistry) {
		this.revisionInfoEntityName = revisionInfoEntityName;
		this.timestampAsDate = timestampAsDate;

		this.revisionInfoClassConstructor = ReflectHelper.getDefaultConstructor( revisionInfoClass );
		this.revisionTimestampSetter = ReflectionTools.getSetter( revisionInfoClass, revisionInfoTimestampData, serviceRegistry );

		this.listenerManagedBean = resolveRevisionListenerBean( listenerClass, serviceRegistry );

		this.sessionCacheCleaner = new SessionCacheCleaner();
	}

	@Override
	public void setRevisionInfoNumberReader(RevisionInfoNumberReader revisionInfoNumberReader) {
		this.revisionInfoNumberReader = revisionInfoNumberReader;
	}

	@Override
	public void saveRevisionData(Session session, Object revisionData) {
		session.save( revisionInfoEntityName, revisionData );
		if ( revisionInfoNumberReader != null && revisionInfoNumberReader.getRevisionNumber( revisionData ).longValue() < 0 ) {
			throw new AuditException( "Negative revision numbers are not allowed" );
		}
		sessionCacheCleaner.scheduleAuditDataRemoval( session, revisionData );
	}

	@Override
	public Object generate() {
		Object revisionInfo;
		try {
			revisionInfo = revisionInfoClassConstructor.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}

		final long timestamp = System.currentTimeMillis();
		revisionTimestampSetter.set( revisionInfo, timestampAsDate ? new Date( timestamp ) : timestamp, null );

		if ( listenerManagedBean != null ) {
			listenerManagedBean.getBeanInstance().newRevision( revisionInfo );
		}

		return revisionInfo;
	}

	@Override
	public void entityChanged(
			Class entityClass,
			String entityName,
			Serializable entityId,
			RevisionType revisionType,
			Object revisionInfo) {
		if ( listenerManagedBean != null ) {
			final RevisionListener listener = listenerManagedBean.getBeanInstance();
			if ( EntityTrackingRevisionListener.class.isInstance( listener ) ) {
				( (EntityTrackingRevisionListener) listener ).entityChanged(
						entityClass,
						entityName,
						entityId,
						revisionType,
						revisionInfo
				);
			}
		}
	}

	private ManagedBean<? extends RevisionListener> resolveRevisionListenerBean(
			Class<? extends RevisionListener> listenerClass,
			ServiceRegistry serviceRegistry) {
		if ( !listenerClass.equals( RevisionListener.class ) ) {
			return serviceRegistry.getService( ManagedBeanRegistry.class ).getBean( listenerClass );
		}
		return null;
	}
}
