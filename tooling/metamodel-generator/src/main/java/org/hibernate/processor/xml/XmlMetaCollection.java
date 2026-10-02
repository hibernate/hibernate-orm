package org.hibernate.processor.xml;

import org.hibernate.processor.model.MetaCollection;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaCollection extends XmlMetaAttribute implements MetaCollection {

	private String collectionType;

	public XmlMetaCollection(XmlMetaEntity parent, String propertyName, String type, String collectionType) {
		super( parent, propertyName, type );
		this.collectionType = collectionType;
	}

	@Override
	public String getMetaType() {
		return collectionType;
	}
}
