/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.internal.tools;

import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeAssociationElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.type.EntityType;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class MappingTools {
	/**
	 * @param componentName Name of the component, that is, name of the property in the entity that references the component.
	 *
	 * @return A prefix for properties in the given component.
	 */
	public static String createComponentPrefix(String componentName) {
		return componentName + "_";
	}

	/**
	 * @param referencePropertyName The name of the property that holds the relation to the entity.
	 *
	 * @return A prefix which should be used to prefix an id mapper for the related entity.
	 */
	public static String createToOneRelationPrefix(String referencePropertyName) {
		return referencePropertyName + "_";
	}

	public static String getReferencedEntityName(AttributeBinding attributeBinding) {
		if ( attributeBinding.getAttribute().isSingular() ) {
			final SingularAttributeBinding singularAttributeBinding = (SingularAssociationAttributeBinding) attributeBinding;
			if ( singularAttributeBinding.isAssociation() ) {
				return SingularAssociationAttributeBinding.class.cast( singularAttributeBinding ).getReferencedEntityName();
			}
		}
		else {
			final PluralAttributeBinding pluralAttributeBinding = (PluralAttributeBinding) attributeBinding;
			if ( pluralAttributeBinding.getPluralAttributeElementBinding().getNature().isAssociation() ) {
				final PluralAttributeAssociationElementBinding associationPluralAttributeElementBinding =
						(PluralAttributeAssociationElementBinding) pluralAttributeBinding.getPluralAttributeElementBinding();
				final EntityType entityType =
						(EntityType) associationPluralAttributeElementBinding
								.getHibernateTypeDescriptor()
								.getResolvedTypeMapping();
				return entityType.getAssociatedEntityName();
			}
		}
		return null;
	}

	/**
	 * @param attributeBinding Persistent property.
	 * @return {@code false} if lack of associated entity shall raise an exception, {@code true} otherwise.
	 */
	public static boolean ignoreNotFound(AttributeBinding attributeBinding) {
		if ( ManyToOneAttributeBinding.class.isInstance( attributeBinding )) {
			return ( (ManyToOneAttributeBinding) attributeBinding ).isIgnoreNotFound();
		}
		else if ( attributeBinding instanceof PluralAttributeBinding ) {
			final PluralAttributeBinding pluralAttributeBinding = (PluralAttributeBinding) attributeBinding;
			final PluralAttributeElementBinding elementBinding = pluralAttributeBinding.getPluralAttributeElementBinding();
			if ( elementBinding.getNature() == PluralAttributeElementNature.ONE_TO_MANY ) {
				// TODO: FIX THIS!!!
				//return !( (PluralAttributeElementBindingOneToMany) elementBinding ).isNotFoundAnException();
			}
		}
		return false;
	}
}
