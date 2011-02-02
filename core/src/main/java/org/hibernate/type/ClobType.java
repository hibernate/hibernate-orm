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

import java.sql.Clob;

import org.hibernate.type.descriptor.java.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link Clob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ClobType extends LobType<Clob> {

	public static final ClobType INSTANCE = new ClobType();

	public ClobType() {
		this(
				org.hibernate.type.descriptor.sql.ClobTypeDescriptor.DEFAULT,
				new AlternativeLobTypes.ClobTypes<Clob,ClobType>( ClobType.class )
		);
	}

	protected ClobType(SqlTypeDescriptor sqlTypeDescriptor,
					   AlternativeLobTypes.ClobTypes<Clob,ClobType> clobTypes) {
		super( sqlTypeDescriptor, ClobTypeDescriptor.INSTANCE, clobTypes );
	}

	public String getName() {
		return "clob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected Clob getReplacement(Clob original, Clob target) {
		return target;
	}
}
