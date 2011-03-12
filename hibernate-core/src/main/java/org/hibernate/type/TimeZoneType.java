/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import java.util.TimeZone;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.TimeZoneTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#VARCHAR VARCHAR} and {@link TimeZone}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TimeZoneType
		extends AbstractSingleColumnStandardBasicType<TimeZone>
		implements LiteralType<TimeZone> {

	public static final TimeZoneType INSTANCE = new TimeZoneType();

	public TimeZoneType() {
		super( VarcharTypeDescriptor.INSTANCE, TimeZoneTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "timezone";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	public String objectToSQLString(TimeZone value, Dialect dialect) throws Exception {
		return StringType.INSTANCE.objectToSQLString( value.getID(), dialect );
	}

}
