/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.boot.model.domain;

import javax.persistence.metamodel.Type.PersistenceType;

/**
 * The representation of an embedded in the application's domain model.  Note
 * that the embedded is different than the embeddable - the embeddable refers
 * to the class, embedded refers one usage of that class as the type for a
 * persistent-attribute, collection-element or collection-index.  Hibernate
 * logically maps the idea that the "owner" of the attributes of the embeddable
 * are defined as the embedded instead - it is the embedded level that defines
 * relation-mapping info.
 *
 * @author Steve Ebersole
 */
public interface EmbeddedMapping extends ManagedTypeMapping, ValueMappingContainer {
	@Override
	EmbeddedValueMapping getValueMapping();

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	String getEmbeddableClassName();
}
