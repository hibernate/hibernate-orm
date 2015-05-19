/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Set;

import org.hibernate.internal.util.xml.XmlDocument;

/**
 * Represents a mapping queued for delayed processing to await
 * processing of an extends entity upon which it depends.
 *
 * @author Steve Ebersole
 */
public class ExtendsQueueEntry {
	private final String explicitName;
	private final String mappingPackage;
	private final XmlDocument metadataXml;
	private final Set<String> entityNames;

	public ExtendsQueueEntry(String explicitName, String mappingPackage, XmlDocument metadataXml, Set<String> entityNames) {
		this.explicitName = explicitName;
		this.mappingPackage = mappingPackage;
		this.metadataXml = metadataXml;
		this.entityNames = entityNames;
	}

	public String getExplicitName() {
		return explicitName;
	}

	public String getMappingPackage() {
		return mappingPackage;
	}

	public XmlDocument getMetadataXml() {
		return metadataXml;
	}

	public Set<String> getEntityNames() {
		return entityNames;
	}
}
