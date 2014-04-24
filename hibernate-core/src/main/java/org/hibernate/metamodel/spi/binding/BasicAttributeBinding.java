/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import java.util.List;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public class BasicAttributeBinding
		extends AbstractSingularAttributeBinding
		implements SingularNonAssociationAttributeBinding {

	private final RelationalValueBindingContainer relationalValueBindingContainer;
	private final PropertyGeneration generation;

	public BasicAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			List<RelationalValueBinding> relationalValueBindings,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			PropertyGeneration generation) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
		this.relationalValueBindingContainer = new RelationalValueBindingContainer( relationalValueBindings );
		this.generation = generation;
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	public PropertyGeneration getGeneration() {
		return generation;
	}

	@Override
	protected RelationalValueBindingContainer getRelationalValueBindingContainer() {
		return relationalValueBindingContainer;
	}

	@Override
	protected void collectRelationalValueBindings(RelationalValueBindingContainer relationalValueBindingContainer) {
		relationalValueBindingContainer.addRelationalValueBindings( this.relationalValueBindingContainer );
	}
}
