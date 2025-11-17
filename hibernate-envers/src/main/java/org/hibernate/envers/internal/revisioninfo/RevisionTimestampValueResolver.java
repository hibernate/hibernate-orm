/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.revisioninfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
			revisionTimestampSetter.set( object, new Date() );
		}
		else if ( timestampData.isTimestampLocalDateTime() ) {
			revisionTimestampSetter.set( object, LocalDateTime.now() );
		}
		else if ( timestampData.isInstant() ) {
			// HHH-17139 truncated to milliseconds to allow Date-based AuditReader functions to
			// continue to work with the same precision level.
			revisionTimestampSetter.set( object, Instant.now().truncatedTo( ChronoUnit.MILLIS ) );
		}
		else {
			revisionTimestampSetter.set( object, System.currentTimeMillis() );
		}
	}

	public Object resolveByValue(Date date) {
		if ( date != null ) {
			if ( timestampData.isTimestampDate() ) {
				return date;
			}
			else if ( timestampData.isTimestampLocalDateTime() ) {
				return LocalDateTime.ofInstant( date.toInstant(), ZoneId.systemDefault() );
			}
			else if ( timestampData.isInstant() ) {
				return date.toInstant();
			}
			else {
				return date.getTime();
			}
		}
		return null;
	}

	public Object resolveByValue(LocalDateTime localDateTime) {
		if ( localDateTime != null ) {
			if ( timestampData.isTimestampDate() ) {
				return Date.from( localDateTime.atZone( ZoneId.systemDefault() ).toInstant() );
			}
			else if ( timestampData.isTimestampLocalDateTime() ) {
				return localDateTime;
			}
			else if ( timestampData.isInstant() ) {
				return localDateTime.atZone( ZoneId.systemDefault() ).toInstant();
			}
			else {
				return localDateTime.atZone( ZoneId.systemDefault() ).toInstant().toEpochMilli();
			}
		}
		return null;
	}

	public Object resolveByValue(Instant instant) {
		if ( instant != null ) {
			if ( timestampData.isTimestampDate() ) {
				return Date.from( instant );
			}
			else if ( timestampData.isTimestampLocalDateTime() ) {
				return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() );
			}
			else if ( timestampData.isInstant() ) {
				return instant;
			}
			else {
				return instant.toEpochMilli();
			}
		}
		return null;
	}
}
