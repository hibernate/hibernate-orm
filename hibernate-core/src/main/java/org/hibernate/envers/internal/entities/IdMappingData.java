/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.envers.internal.entities.mapper.id.IdMapper;

import org.dom4j.Element;

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
