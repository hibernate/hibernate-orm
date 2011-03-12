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

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A base type used to define a LOB type; it also provides
 * alternatives that can override this type via
 * {@link org.hibernate.type.LobType#getAlternatives()}   getAlternatives()}
 *
 * @author Gail Badner
 */
public abstract class LobType<T> extends AbstractSingleColumnStandardBasicType<T> {
	private AlternativeLobTypes alternativeLobTypes;

	public LobType(SqlTypeDescriptor sqlTypeDescriptor,
				   JavaTypeDescriptor<T> javaTypeDescriptor,
				   AlternativeLobTypes alternativeLobTypes) {
		super( sqlTypeDescriptor, javaTypeDescriptor );
		this.alternativeLobTypes = alternativeLobTypes;
	}

	public AlternativeLobTypes getAlternatives() {
		return alternativeLobTypes;
	}
}
