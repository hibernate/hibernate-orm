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

import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.domain.Attribute;

/**
 * The basic contract for binding a {@link #getAttribute() attribute} from the domain model to the relational model.
 *
 * @author Steve Ebersole
 */
public interface AttributeBinding {
	/**
	 * Obtain the entity binding to which this attribute binding exists.
	 *
	 * @return The entity binding.
	 */
	public AttributeBindingContainer getContainer();

	/**
	 * Obtain the attribute bound.
	 *
	 * @return The attribute.
	 */
	public Attribute getAttribute();

	/**
	 * Obtain the descriptor for the Hibernate {@link org.hibernate.type.Type} for this binding.
	 * <p/>
	 * For information about the Java type, query the {@link org.hibernate.metamodel.spi.domain.Attribute}
	 * obtained from {@link #getAttribute()} instead.
	 *
	 * @return The type descriptor
	 */
	public HibernateTypeDescriptor getHibernateTypeDescriptor();

	public boolean isAssociation();

	public boolean isCascadeable();

	public boolean isBackRef();

	public boolean isBasicPropertyAccessor();

	public String getPropertyAccessorName();

	public boolean isIncludedInOptimisticLocking();

	/**
	 * Obtain the meta attributes associated with this binding
	 *
	 * @return The meta attributes
	 */
	public MetaAttributeContext getMetaAttributeContext();

	public boolean isAlternateUniqueKey();

	public boolean isLazy();

	public AttributePath getAttributePath();
	public AttributeRole getAttributeRole();
}
