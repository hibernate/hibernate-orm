/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import javax.lang.model.element.Element;

import org.hibernate.jpamodelgen.model.MetaCollection;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class AnnotationMetaCollection extends AnnotationMetaAttribute implements MetaCollection {
	private String collectionType;

	public AnnotationMetaCollection(AnnotationMetaEntity parent, Element element, String collectionType, String elementType) {
		super( parent, element, elementType );
		this.collectionType = collectionType;
	}

	@Override
	public String getMetaType() {
		return collectionType;
	}
}
