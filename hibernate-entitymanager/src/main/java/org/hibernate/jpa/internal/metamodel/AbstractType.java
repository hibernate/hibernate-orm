/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.internal.metamodel;

import java.io.Serializable;
import javax.persistence.metamodel.Type;

/**
 * Defines commonality for the JPA {@link Type} hierarchy of interfaces.
 *
 * @author Steve Ebersole
 * @author Brad Koehn
 */
public abstract class AbstractType<X> implements Type<X>, Serializable {
    private final Class<X> javaType;
    private final String typeName;

	/**
	 * Instantiates the type based on the given Java type.
	 *
	 * @param javaType
	 * @param typeName
	 */
	protected AbstractType(Class<X> javaType, String typeName) {
		this.javaType = javaType;
		this.typeName = typeName == null ? "unknown" : typeName;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * IMPL NOTE : The Hibernate version may return {@code null} here in the case of either dynamic models or
	 * entity classes mapped multiple times using entity-name.  In these cases, the {@link #getTypeName()} value
	 * should be used.
	 */
	@Override
    public Class<X> getJavaType() {
        return javaType;
    }

	/**
	 * Obtains the type name.  See notes on {@link #getJavaType()} for details
	 *
	 * @return The type name
	 */
    public String getTypeName() {
        return typeName;
    }
}
