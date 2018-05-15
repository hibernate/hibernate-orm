/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.revisioninfo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.hibernate.envers.internal.entities.RevisionTimestampData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Chris Cranford
 * @since 6.0
 */
public class RevisionTimestampValueResolver {

	private final RevisionTimestampData timestampData;
	private final Setter revisionTimestampSetter;

	public RevisionTimestampValueResolver(Class<?> revisionInfoClass, RevisionTimestampData timestampData, ServiceRegistry serviceRegistry) {
		this.timestampData = timestampData;
		this.revisionTimestampSetter = ReflectionTools.getSetter( revisionInfoClass, timestampData, serviceRegistry );
	}

	public String getName() {
		return timestampData.getName();
	}

	public void resolveNow(Object object) {
		if ( timestampData.isTimestampDate() ) {
			revisionTimestampSetter.set( object, new Date(), null );
		}
		else if ( timestampData.isTimestampLocalDateTime() ) {
			revisionTimestampSetter.set( object, LocalDateTime.now(), null );
		}
		else {
			revisionTimestampSetter.set( object, System.currentTimeMillis(), null );
		}
	}

	public Object resolveByValue(final Date date) {
		if ( date != null ) {
			if ( timestampData.isTimestampDate() ) {
				return date;
			}
			else if ( timestampData.isTimestampLocalDateTime() ) {
				return LocalDateTime.ofInstant( date.toInstant(), ZoneId.systemDefault() );
			}
			else {
				return date.getTime();
			}
		}
		return null;
	}

	public Object resolveByValue(final LocalDateTime localDateTime) {
		if ( localDateTime != null ) {
			if ( timestampData.isTimestampDate() ) {
				return Date.from( localDateTime.atZone( ZoneId.systemDefault() ).toInstant() );
			}
			else if ( timestampData.isTimestampLocalDateTime() ) {
				return localDateTime;
			}
			else {
				return localDateTime.atZone( ZoneId.systemDefault() ).toInstant().getEpochSecond();
			}
		}
		return null;
	}
}
