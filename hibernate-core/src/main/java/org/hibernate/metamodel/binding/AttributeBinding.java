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
package org.hibernate.metamodel.binding;

import java.util.Set;

import org.hibernate.metamodel.binder.source.MetaAttributeContext;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Value;

/**
 * The basic contract for binding between an {@link #getAttribute() attribute} and a {@link #getValue() value}
 *
 * @author Steve Ebersole
 */
public interface AttributeBinding {
	/**
	 * Obtain the entity binding to which this attribute binding exists.
	 *
	 * @return The entity binding.
	 */
	public EntityBinding getEntityBinding();

	/**
	 * Obtain the attribute bound.
	 *
	 * @return The attribute.
	 */
	public Attribute getAttribute();

	/**
	 * Obtain the value bound
	 *
	 * @return The value
	 */
	public Value getValue();

	/**
	 * Obtain the descriptor for the Hibernate Type for this binding.
	 *
	 * @return The type descriptor
	 */
	public HibernateTypeDescriptor getHibernateTypeDescriptor();

	/**
	 * Obtain the meta attributes associated with this binding
	 *
	 * @return The meta attributes
	 */
	public MetaAttributeContext getMetaAttributeContext();

	/**
	 * Returns the number of {@link org.hibernate.metamodel.relational.SimpleValue}
	 * objects that will be returned by {@link #getValues()}
	 *
	 * @return the number of objects that will be returned by {@link #getValues()}.
	 *
	 * @see {@link org.hibernate.metamodel.relational.SimpleValue}
	 * @see {@link #getValues()}
	 */
	public int getValuesSpan();

	/**
	 * @return In the case that {@link #getValue()} represents a {@link org.hibernate.metamodel.relational.Tuple} this method
	 *         gives access to its compound values.  In the case of {@link org.hibernate.metamodel.relational.SimpleValue},
	 *         we return an Iterable over that single simple value.
	 */
	public Iterable<SimpleValue> getValues();

	public String getPropertyAccessorName();

	public boolean isBasicPropertyAccessor();

	public boolean hasFormula();

	public boolean isAlternateUniqueKey();

	public boolean isNullable();

	public boolean[] getColumnUpdateability();

	public boolean[] getColumnInsertability();

	public boolean isSimpleValue();

	public boolean isLazy();

	public void addEntityReferencingAttributeBinding(EntityReferencingAttributeBinding attributeBinding);

	public Set<EntityReferencingAttributeBinding> getEntityReferencingAttributeBindings();

	public void validate();
}
