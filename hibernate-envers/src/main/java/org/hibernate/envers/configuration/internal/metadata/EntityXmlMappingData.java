/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
