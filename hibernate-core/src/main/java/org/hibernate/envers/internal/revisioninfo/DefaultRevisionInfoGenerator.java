/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.revisioninfo;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DefaultRevisionInfoGenerator implements RevisionInfoGenerator {
	private final String revisionInfoEntityName;
	private final RevisionListener listener;
	private final Setter revisionTimestampSetter;
	private final boolean timestampAsDate;
	private final Class<?> revisionInfoClass;
	private final SessionCacheCleaner sessionCacheCleaner;

	public DefaultRevisionInfoGenerator(
			String revisionInfoEntityName,
			Class<?> revisionInfoClass,
			Class<? extends RevisionListener> listenerClass,
			PropertyData revisionInfoTimestampData,
			boolean timestampAsDate,
			ServiceRegistry serviceRegistry) {
		this.revisionInfoEntityName = revisionInfoEntityName;
		this.revisionInfoClass = revisionInfoClass;
		this.timestampAsDate = timestampAsDate;

		revisionTimestampSetter = ReflectionTools.getSetter( revisionInfoClass, revisionInfoTimestampData, serviceRegistry );

		if ( !listenerClass.equals( RevisionListener.class ) ) {
			// This is not the default value.
			try {
				listener = (RevisionListener) ReflectHelper.getDefaultConstructor( listenerClass ).newInstance();
			}
			catch (InstantiationException e) {
				throw new MappingException( e );
			}
			catch (IllegalAccessException e) {
				throw new MappingException( e );
			}
			catch (InvocationTargetException e) {
				throw new MappingException( e );
			}
		}
		else {
			// Default listener - none
			listener = null;
		}

		sessionCacheCleaner = new SessionCacheCleaner();
	}

	@Override
	public void saveRevisionData(Session session, Object revisionData) {
		session.save( revisionInfoEntityName, revisionData );
		sessionCacheCleaner.scheduleAuditDataRemoval( session, revisionData );
	}

	@Override
	public Object generate() {
		Object revisionInfo;
		try {
			revisionInfo = ReflectHelper.getDefaultConstructor( revisionInfoClass ).newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}

		final long timestamp = System.currentTimeMillis();
		revisionTimestampSetter.set( revisionInfo, timestampAsDate ? new Date( timestamp ) : timestamp, null );

		if ( listener != null ) {
			listener.newRevision( revisionInfo );
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
		if ( listener instanceof EntityTrackingRevisionListener ) {
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
