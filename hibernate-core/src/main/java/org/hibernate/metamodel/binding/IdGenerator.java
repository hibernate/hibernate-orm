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

import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Identifier generator container, Useful to keep named generator in annotations
 *
 * @author Emmanuel Bernard
 */
public class IdGenerator implements Serializable {
    private final String name;
    private final String strategy;
    private final Map<String, String> parameters;

    public IdGenerator( String name,
                        String strategy,
                        Map<String, String> parameters ) {
        this.name = name;
        this.strategy = strategy;
        if ( CollectionHelper.isEmpty( parameters ) ) {
            this.parameters = Collections.emptyMap();
        }
        else {
            this.parameters = Collections.unmodifiableMap( parameters );
        }
    }

    /**
     * @return identifier generator strategy
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * @return generator name
     */
    public String getName() {
        return name;
    }

    /**
     * @return generator configuration parameters
     */
    public Map<String, String> getParameters() {
		return parameters;
    }
}
