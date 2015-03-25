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

import java.time.Duration;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.DurationJavaDescriptor;
import org.hibernate.type.descriptor.sql.BigIntTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DurationType
		extends AbstractSingleColumnStandardBasicType<Duration>
		implements LiteralType<Duration> {
	/**
	 * Singleton access
	 */
	public static final DurationType INSTANCE = new DurationType();

	public DurationType() {
		super( BigIntTypeDescriptor.INSTANCE, DurationJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(Duration value, Dialect dialect) throws Exception {
		return String.valueOf( value.toNanos() );
	}

	@Override
	public String getName() {
		return Duration.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
