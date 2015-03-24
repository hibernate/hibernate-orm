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

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetDateTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetDateTime>
		implements VersionType<OffsetDateTime>, LiteralType<OffsetDateTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetDateTimeType INSTANCE = new OffsetDateTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S xxxxx", Locale.ENGLISH );

	public OffsetDateTimeType() {
		super( TimestampTypeDescriptor.INSTANCE, OffsetDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(OffsetDateTime value, Dialect dialect) throws Exception {
		return "{ts '" + FORMATTER.format( value ) + "'}";
	}

	@Override
	public OffsetDateTime seed(SessionImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	public OffsetDateTime next(OffsetDateTime current, SessionImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<OffsetDateTime> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public String getName() {
		return OffsetDateTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
