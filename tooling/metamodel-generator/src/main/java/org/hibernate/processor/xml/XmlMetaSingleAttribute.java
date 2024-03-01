/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.xml;

import org.hibernate.processor.model.MetaSingleAttribute;
import org.hibernate.processor.util.Constants;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaSingleAttribute extends XmlMetaAttribute implements MetaSingleAttribute {

	public XmlMetaSingleAttribute(XmlMetaEntity parent, String propertyName, String type) {
		super( parent, propertyName, type );
	}

	@Override
	public String getMetaType() {
		return Constants.SINGULAR_ATTRIBUTE;
	}
}
