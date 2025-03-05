/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
