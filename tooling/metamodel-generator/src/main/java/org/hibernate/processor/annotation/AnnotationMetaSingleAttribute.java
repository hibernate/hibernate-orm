/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import javax.lang.model.element.Element;

import org.hibernate.processor.model.MetaSingleAttribute;
import org.hibernate.processor.util.Constants;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaSingleAttribute extends AnnotationMetaAttribute implements MetaSingleAttribute {

	public AnnotationMetaSingleAttribute(AnnotationMetaEntity parent, Element element, String type) {
		super( parent, element, type );
	}

	@Override
	public final String getMetaType() {
		return Constants.SINGULAR_ATTRIBUTE;
	}
}
