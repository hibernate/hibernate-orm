/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * Basic support for {@link AttributeBinding} implementors
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeBinding implements AttributeBinding {
	private final AttributeBindingContainer container;
	private final Attribute attribute;

	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final Set<SingularAssociationAttributeBinding> entityReferencingAttributeBindings = new HashSet<SingularAssociationAttributeBinding>();

	private final String propertyAccessorName;
	private final boolean includedInOptimisticLocking;

	private boolean isAlternateUniqueKey;

	private final MetaAttributeContext metaAttributeContext;
	private final ValueHolder<String> roleHolder;

	protected AbstractAttributeBinding(
			AttributeBindingContainer container,
			Attribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			MetaAttributeContext metaAttributeContext) {
		this.container = container;
		this.attribute = attribute;
		this.propertyAccessorName = propertyAccessorName;
		this.includedInOptimisticLocking = includedInOptimisticLocking;
		this.metaAttributeContext = metaAttributeContext;
		this.roleHolder = new ValueHolder<String>(
				new ValueHolder.DeferredInitializer<String>() {
					@Override
					public String initialize() {
						return getContainer().getPathBase() + '.' + getAttribute().getName();
					}
				}
		);
	}

	@Override
	public AttributeBindingContainer getContainer() {
		return container;
	}

	@Override
	public Attribute getAttribute() {
		return attribute;
	}

	protected String getRole() {
		return roleHolder.getValue();
	}

	@Override
	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	@Override
	public boolean isBackRef() {
		return false;
	}

	@Override
	public boolean isBasicPropertyAccessor() {
		return propertyAccessorName == null || "property".equals( propertyAccessorName );
	}

	@Override
	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return includedInOptimisticLocking;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		// todo : is this the same as "part of natural id"?
		return isAlternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean alternateUniqueKey) {
		this.isAlternateUniqueKey = alternateUniqueKey;
	}

	public void addEntityReferencingAttributeBinding(SingularAssociationAttributeBinding referencingAttributeBinding) {
		entityReferencingAttributeBindings.add( referencingAttributeBinding );
	}

	public Set<SingularAssociationAttributeBinding> getEntityReferencingAttributeBindings() {
		return Collections.unmodifiableSet( entityReferencingAttributeBindings );
	}

	public void validate() {
		if ( !entityReferencingAttributeBindings.isEmpty() ) {
			// TODO; validate that this AttributeBinding can be a target of an entity reference
			// (e.g., this attribute is the primary key or there is a unique-key)
			// can a unique attribute be used as a target? if so, does it need to be non-null?
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AbstractAttributeBinding" );
		sb.append( "{attribute=" ).append( attribute.getName() );
		sb.append( '}' );
		return sb.toString();
	}
}
