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
package org.jboss.envers.configuration.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.envers.ModificationStore;
import org.jboss.envers.VersionsJoinTable;
import org.jboss.envers.VersionsTable;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
*/
public class PersistentClassVersioningData {
    public PersistentClassVersioningData() {
        propertyStoreInfo = new PropertyStoreInfo(new HashMap<String, ModificationStore>());
        secondaryTableDictionary = new HashMap<String, String>();
        unversionedProperties = new ArrayList<String>();
        versionsJoinTables = new HashMap<String, VersionsJoinTable>();
        mapKeys = new HashMap<String, String>();
    }

    public PropertyStoreInfo propertyStoreInfo;
    public VersionsTable versionsTable;
    public Map<String, String> secondaryTableDictionary;
    public List<String> unversionedProperties;
    /**
     * A map from property names to custom join tables definitions.
     */
    public Map<String, VersionsJoinTable> versionsJoinTables;
    /**
     * A map from property names to the value of the related property names in a map key annotation. An empty string,
     * if the property name is not specified in the mapkey annotation.
     */
    public Map<String, String> mapKeys;

    public boolean isVersioned() {
        if (propertyStoreInfo.propertyStores.size() > 0) { return true; }
        if (propertyStoreInfo.defaultStore != null) { return true; }
        return false;
    }
}
