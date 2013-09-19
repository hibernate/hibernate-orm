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
package org.hibernate.type;

import java.util.Calendar;

import org.hibernate.type.descriptor.java.CalendarTimeTypeDescriptor;
import org.hibernate.type.descriptor.sql.TimeTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#TIME TIME} and {@link Calendar}.
 * <p/>
 * For example, a Calendar attribute annotated with {@link javax.persistence.Temporal} and specifying
 * {@link javax.persistence.TemporalType#TIME}
 *
 * @author Steve Ebersole
 */
public class CalendarTimeType extends AbstractSingleColumnStandardBasicType<Calendar> {
	public static final CalendarTimeType INSTANCE = new CalendarTimeType();

	public CalendarTimeType() {
		super( TimeTypeDescriptor.INSTANCE, CalendarTimeTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "calendar_time";
	}
}
