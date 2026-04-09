/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ide.completion;

/**
 * Class that represents an alias to some entityname in a HQL statement. e.g. "Product as p" or "Product p"
 * 
 * Should not be used by external clients.
 * 
 * @author leon, Max Rydahl Andersen
 */
public class EntityNameReference {

    private String alias;

    private String entityName;

    public EntityNameReference(String type, String alias) {
        this.entityName = type;
        this.alias = alias;
    }

    /** 
     * 
     * @return The alias, the "p" in "Product as p" 
     */
    public String getAlias() {
        return alias;
    }

    /**
     * 
     * @return the entityname, the "Product" in "Product as b"
     */
    public String getEntityName() {
        return entityName;
    }

    public String toString() {
        return alias + ":" + entityName;
    }



}
