/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Represents the metamodel view of a typedef (type definition).
 *
 * @author John Verhaeg
 */
public class TypeDef implements Serializable {
	private final String name;
    private final String typeClass;
    private final Map<String, String> parameters;

	public TypeDef(String name, String typeClass, Map<String, String> parameters) {
		this.name = name;
		this.typeClass = typeClass;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public String getTypeClass() {
        return typeClass;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }
}
