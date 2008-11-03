/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.entities;

import org.hibernate.envers.ModificationStore;

/**
 * Holds information on a property that is audited.
 * @author Adam Warski (adam at warski dot org)
 */
public class PropertyData {
    private final String name;
    private final String accessType;
    private final ModificationStore store;

    /**
     * Copies the given property data, except the name.
     * @param newName New name.
     * @param propertyData Property data to copy the rest of properties from.
     */
    public PropertyData(String newName, PropertyData propertyData) {
        this.name = newName;
        this.accessType = propertyData.accessType;
        this.store = propertyData.store;
    }

    /**
     * @param name Name of the property.
     * @param accessType Accessor type for this property.
     * @param store How this property should be stored.
     */
    public PropertyData(String name, String accessType, ModificationStore store) {
        this.name = name;
        this.accessType = accessType;
        this.store = store;
    }

    public String getName() {
        return name;
    }

    public String getAccessType() {
        return accessType;
    }

    public ModificationStore getStore() {
        return store;
    }
}
