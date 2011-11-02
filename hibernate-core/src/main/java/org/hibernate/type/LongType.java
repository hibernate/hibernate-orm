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

import java.io.Serializable;
import java.util.Comparator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.sql.BigIntTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#BIGINT BIGINT} and {@link Long}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class LongType
		extends AbstractSingleColumnStandardBasicType<Long>
		implements PrimitiveType<Long>, DiscriminatorType<Long>, VersionType<Long> {

	public static final LongType INSTANCE = new LongType();

	@SuppressWarnings({ "UnnecessaryBoxing" })
	private static final Long ZERO = Long.valueOf( 0 );

	public LongType() {
		super( BigIntTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "long";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), long.class.getName(), Long.class.getName() };
	}

	public Serializable getDefaultValue() {
		return ZERO;
	}

	public Class getPrimitiveClass() {
		return long.class;
	}

	public Long stringToObject(String xml) throws Exception {
		return Long.valueOf( xml );
	}

	@SuppressWarnings({ "UnnecessaryBoxing", "UnnecessaryUnboxing" })
	public Long next(Long current, SessionImplementor session) {
		return current + 1l;
	}

	public Long seed(SessionImplementor session) {
		return ZERO;
	}

	public Comparator<Long> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}
	
	public String objectToSQLString(Long value, Dialect dialect) throws Exception {
		return value.toString();
	}
}
