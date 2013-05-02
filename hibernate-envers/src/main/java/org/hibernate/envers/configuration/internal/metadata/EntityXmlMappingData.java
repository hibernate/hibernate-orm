/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.configuration.internal.metadata;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntityXmlMappingData {
	private Document mainXmlMapping;
	private List<Document> additionalXmlMappings;
	/**
	 * The xml element that maps the class. The root can be one of the folowing elements:
	 * class, subclass, union-subclass, joined-subclass
	 */
	private Element classMapping;

	public EntityXmlMappingData() {
		mainXmlMapping = DocumentHelper.createDocument();
		additionalXmlMappings = new ArrayList<Document>();
	}

	public Document getMainXmlMapping() {
		return mainXmlMapping;
	}

	public List<Document> getAdditionalXmlMappings() {
		return additionalXmlMappings;
	}

	public Document newAdditionalMapping() {
		Document additionalMapping = DocumentHelper.createDocument();
		additionalXmlMappings.add( additionalMapping );

		return additionalMapping;
	}

	public Element getClassMapping() {
		return classMapping;
	}

	public void setClassMapping(Element classMapping) {
		this.classMapping = classMapping;
	}
}
