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
 *
 */
package org.hibernate.envers.configuration.metadata;

import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.entities.PropertyData;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentPropertyAuditingData {
    private String name;
    private ModificationStore store;
    private String mapKey;
    private AuditJoinTable joinTable;
    private String accessType;

    public PersistentPropertyAuditingData() {
    }

    public PersistentPropertyAuditingData(String name, String accessType, ModificationStore store) {
        this.name = name;
        this.accessType = accessType;
        this.store = store;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ModificationStore getStore() {
        return store;
    }

    public void setStore(ModificationStore store) {
        this.store = store;
    }

    public String getMapKey() {
        return mapKey;
    }

    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }

    public AuditJoinTable getJoinTable() {
        return joinTable;
    }

    public void setJoinTable(AuditJoinTable joinTable) {
        this.joinTable = joinTable;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public PropertyData getPropertyData() {
        return new PropertyData(name, name, accessType, store);
    }
}
