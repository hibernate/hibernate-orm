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

import org.dom4j.Element;
import org.hibernate.envers.entities.mapper.id.IdMapper;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class IdMappingData {
    private final IdMapper idMapper;
    // Mapping which will be used to generate the entity
    private final Element xmlMapping;
    // Mapping which will be used to generate references to the entity in related entities
    private final Element xmlRelationMapping;

    public IdMappingData(IdMapper idMapper, Element xmlMapping, Element xmlRelationMapping) {
        this.idMapper = idMapper;
        this.xmlMapping = xmlMapping;
        this.xmlRelationMapping = xmlRelationMapping;
    }

    public IdMapper getIdMapper() {
        return idMapper;
    }

    public Element getXmlMapping() {
        return xmlMapping;
    }

    public Element getXmlRelationMapping() {
        return xmlRelationMapping;
    }
}
