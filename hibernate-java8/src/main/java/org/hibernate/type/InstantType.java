/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.InstantJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.time.LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class InstantType
		extends AbstractSingleColumnStandardBasicType<Instant>
		implements VersionType<Instant>, LiteralType<Instant> {
	/**
	 * Singleton access
	 */
	public static final InstantType INSTANCE = new InstantType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S 'Z'", Locale.ENGLISH );

	public InstantType() {
		super( TimestampTypeDescriptor.INSTANCE, InstantJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(Instant value, Dialect dialect) throws Exception {
		return "{ts '" + FORMATTER.format( ZonedDateTime.ofInstant( value, ZoneId.of( "UTC" ) ) ) + "'}";
	}

	@Override
	public Instant seed(SessionImplementor session) {
		return Instant.now();
	}

	@Override
	public Instant next(Instant current, SessionImplementor session) {
		return Instant.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<Instant> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public String getName() {
		return Instant.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
