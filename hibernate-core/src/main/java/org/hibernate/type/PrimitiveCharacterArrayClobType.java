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

import org.hibernate.type.descriptor.java.PrimitiveCharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Map a char[] to a Clob
 *
 * @author Emmanuel Bernard
 */
public class PrimitiveCharacterArrayClobType extends LobType<char[]> {
	public static final PrimitiveCharacterArrayClobType INSTANCE = new PrimitiveCharacterArrayClobType();

	public PrimitiveCharacterArrayClobType() {
		this(
				ClobTypeDescriptor.DEFAULT,
				new AlternativeLobTypes.ClobTypes<char[],PrimitiveCharacterArrayClobType>(
						PrimitiveCharacterArrayClobType.class
				)
		);
	}

	protected PrimitiveCharacterArrayClobType(SqlTypeDescriptor sqlTypeDescriptor, AlternativeLobTypes.
			ClobTypes<char[],PrimitiveCharacterArrayClobType> clobTypes) {
		super( sqlTypeDescriptor, PrimitiveCharacterArrayTypeDescriptor.INSTANCE, clobTypes );
	}

	public String getName() {
		return "characters_clob";
	}
}
