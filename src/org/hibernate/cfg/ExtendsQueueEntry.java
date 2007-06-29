package org.hibernate.cfg;

import org.dom4j.Document;

/**
 * Represents a mapping queued for delayed processing to await
 * processing of an extends entity upon which it depends.
 *
 * @author Steve Ebersole
 */
public class ExtendsQueueEntry {
	private final String explicitName;
	private final String mappingPackage;
	private final Document document;

	public ExtendsQueueEntry(String explicitName, String mappingPackage, Document document) {
		this.explicitName = explicitName;
		this.mappingPackage = mappingPackage;
		this.document = document;
	}

	public String getExplicitName() {
		return explicitName;
	}

	public String getMappingPackage() {
		return mappingPackage;
	}

	public Document getDocument() {
		return document;
	}
}
