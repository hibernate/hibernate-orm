/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.metamodel.CollectionClassification;

/**
 * JAXB marshalling for {@link CollectionClassification}
 *
 * @author Steve Ebersole
 */
public class CollectionClassificationMarshalling {
	public static CollectionClassification fromXml(String name) {
		return name == null ? null : CollectionClassification.interpretSetting( name.replace( '-', '_' ) );
	}

	public static String toXml(CollectionClassification classification) {
		return classification == null ? null : classification.name().replace( '_', '-' );
	}
}
