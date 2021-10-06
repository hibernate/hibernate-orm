/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.model.domain.JpaMetamodel;

/**
 * Access to Hibernate's runtime metamodels which includes its domain-model (JPA impl) and its
 * relational-mapping model
 *
 * @author Steve Ebersole
 */
@Incubating
public interface RuntimeMetamodels {
	/**
	 * Access to the JPA / domain metamodel
	 */
	JpaMetamodel getJpaMetamodel();

	/**
	 * Access to the relational-mapping model
	 */
	MappingMetamodel getMappingMetamodel();


	// some convenience methods...

	default EntityMappingType getEntityMappingType(String entityName) {
		return getMappingMetamodel().getEntityDescriptor( entityName );
	}

	default EntityMappingType getEntityMappingType(Class entityType) {
		return getMappingMetamodel().getEntityDescriptor( entityType );
	}

	default PluralAttributeMapping getPluralAttributeMapping(String role) {
		return getMappingMetamodel().findCollectionDescriptor( role ).getAttributeMapping();
	}

	// todo (6.0) : I think we might need a form of mapping-model look-up for embeddables, something like:
	EmbeddableValuedModelPart getEmbedded(String role);

	default String getImportedName(String name) {
		return getMappingMetamodel().getImportedName( name );
	}
}
