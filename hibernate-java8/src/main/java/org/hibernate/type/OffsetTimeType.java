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

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimeTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetTime>
		implements VersionType<OffsetTime>, LiteralType<OffsetTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetTimeType INSTANCE = new OffsetTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "HH:mm:ss.S xxxxx", Locale.ENGLISH );

	public OffsetTimeType() {
		super( TimeTypeDescriptor.INSTANCE, OffsetTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(OffsetTime value, Dialect dialect) throws Exception {
		return "{t '" + FORMATTER.format( value ) + "'}";
	}

	@Override
	public OffsetTime seed(SessionImplementor session) {
		return OffsetTime.now();
	}

	@Override
	public OffsetTime next(OffsetTime current, SessionImplementor session) {
		return OffsetTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<OffsetTime> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public String getName() {
		return OffsetTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
