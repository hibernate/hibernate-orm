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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ZonedDateTimeType
		extends AbstractSingleColumnStandardBasicType<ZonedDateTime>
		implements VersionType<ZonedDateTime>, LiteralType<ZonedDateTime> {

	/**
	 * Singleton access
	 */
	public static final ZonedDateTimeType INSTANCE = new ZonedDateTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S VV", Locale.ENGLISH );

	public ZonedDateTimeType() {
		super( TimestampTypeDescriptor.INSTANCE, ZonedDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(ZonedDateTime value, Dialect dialect) throws Exception {
		return "{ts '" + FORMATTER.format( value ) + "'}";
	}

	@Override
	public ZonedDateTime seed(SessionImplementor session) {
		return ZonedDateTime.now();
	}

	@Override
	public ZonedDateTime next(ZonedDateTime current, SessionImplementor session) {
		return ZonedDateTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<ZonedDateTime> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public String getName() {
		return ZonedDateTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
