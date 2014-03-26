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
package org.hibernate.metamodel.internal.resolver;

import java.util.List;

import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 + * @author Gail Badner
 + */
public interface AssociationRelationalBindingResolver {

	SingularAttributeBinding resolveOneToOneReferencedAttributeBinding(
			final ToOneAttributeSource attributeSource,
			final EntityBinding referencedEntityBinding);

	List<RelationalValueBinding> resolveOneToOneRelationalValueBindings(
			final ToOneAttributeSource attributeSource,
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeBinding referencedAttributeBinding);

	ForeignKey resolveOneToOneForeignKey(
			ToOneAttributeSource attributeSource,
			TableSpecification sourceTable,
			List<Column> sourceColumns,
			EntityBinding referencedEntityBinding);

	SingularAttributeBinding resolveManyToOneReferencedAttributeBinding(
			final AttributeBindingContainer attributeBindingContainer,
			final ToOneAttributeSource attributeSource,
			final EntityBinding referencedEntityBinding);

	// TODO: referencedAttributeBinding and referencedEntityBinding are both included due to a bug that can
	// cause entityBinding !=  entityBinding.hierarchyDetails.getEntityIdentifier().getAttributeBinding().getContainer()
	List<RelationalValueBinding> resolveManyToOneRelationalValueBindings(
			final ToOneAttributeSource attributeSource,
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeBinding referencedAttributeBinding,
			final EntityBinding referencedEntityBinding);

	ForeignKey resolveManyToOneForeignKey(
			ToOneAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			List<RelationalValueBinding> relationalValueBindings,
			EntityBinding referencedEntityBinding);

	List<RelationalValueBinding> resolveManyToManyElementRelationalValueBindings(
			final EntityBinding entityBinding,
			final PluralAttributeElementSourceManyToMany elementSource,
			final TableSpecification collectionTable,
			final EntityBinding referencedEntityBinding);

	ForeignKey resolveManyToManyElementForeignKey(
			final EntityBinding entityBinding,
			final PluralAttributeElementSourceManyToMany elementSource,
			final TableSpecification collectionTable,
			final List<RelationalValueBinding> relationalValueBindings,
			final EntityBinding referencedEntityBinding);

	TableSpecification resolveManyToManyCollectionTable(
			PluralAttributeSource pluralAttributeSource,
			String attributePath,
			EntityBinding entityBinding,
			EntityBinding referencedEntityBinding);

	List<RelationalValueBinding> resolvePluralAttributeKeyRelationalValueBindings(
			final PluralAttributeSource attributeSource,
			final EntityBinding entityBinding,
			final TableSpecification collectionTable,
			final EntityBinding referencedEntityBinding);

	ForeignKey resolvePluralAttributeKeyForeignKey(
			final PluralAttributeSource attributeSource,
			final EntityBinding entityBinding,
			final TableSpecification collectionTable,
			final List<RelationalValueBinding> relationalValueBindings,
			final EntityBinding referencedEntityBinding);

	SingularAttributeBinding resolvePluralAttributeKeyReferencedBinding(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource);
}
