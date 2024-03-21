/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.Map;

import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.ClassDetails;

/**
 * Registration for a {@linkplain org.hibernate.usertype.UserCollectionType}
 *
 * @see org.hibernate.annotations.CollectionTypeRegistration
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeImpl
 *
 * @author Steve Ebersole
 */
public class CollectionTypeRegistration {
	private final CollectionClassification classification;
	private final ClassDetails userTypeClass;
	private final Map<String,String> parameterMap;

	public CollectionTypeRegistration(
			CollectionClassification classification,
			ClassDetails userTypeClass,
			Map<String, String> parameterMap) {
		this.classification = classification;
		this.userTypeClass = userTypeClass;
		this.parameterMap = parameterMap;
	}

	public CollectionClassification getClassification() {
		return classification;
	}

	public ClassDetails getUserTypeClass() {
		return userTypeClass;
	}

	public Map<String, String> getParameterMap() {
		return parameterMap;
	}
}
