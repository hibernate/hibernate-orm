/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.xml;

import org.hibernate.jpamodelgen.model.MetaCollection;

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
