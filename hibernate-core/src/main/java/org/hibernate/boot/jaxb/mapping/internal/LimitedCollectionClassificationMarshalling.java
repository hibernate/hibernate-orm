/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import java.util.Locale;

import org.hibernate.boot.internal.LimitedCollectionClassification;

import jakarta.persistence.AccessType;

/**
 * JAXB marshalling for JPA's {@link AccessType}
 *
 * @author Steve Ebersole
 */
public class LimitedCollectionClassificationMarshalling {
	public static LimitedCollectionClassification fromXml(String name) {
		return name == null ? null : LimitedCollectionClassification.valueOf( name.toUpperCase( Locale.ROOT ) );
	}

	public static String toXml(LimitedCollectionClassification classification) {
		return classification == null ? null : classification.name().toLowerCase( Locale.ROOT );
	}
}
