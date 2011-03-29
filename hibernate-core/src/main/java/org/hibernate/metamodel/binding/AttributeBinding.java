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

import java.util.Map;

import org.hibernate.FetchMode;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.TableSpecification;
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
	 * Set the attribute being bound.
	 *
	 * @param attribute The attribute
	 */
	public void setAttribute(Attribute attribute);

	/**
	 * Obtain the value bound
	 *
	 * @return The value
	 */
	public Value getValue();

	/**
	 * Set the value being bound.
	 *
	 * @param value The value
	 */
	public void setValue(Value value);

	/**
	 * Obtain the descriptor for the Hibernate Type for this binding.
	 *
	 * @return The type descriptor
	 */
	public HibernateTypeDescriptor getHibernateTypeDescriptor();

	/**
	 * Obtain the map of meta attributes associated with this binding
	 *
	 * @return The meta attributes
	 */
	public Map<String, MetaAttribute> getMetaAttributes();

	/**
	 * In the case that {@link #getValue()} represents a {@link org.hibernate.metamodel.relational.Tuple} this method
	 * gives access to its compound values.  In the case of {@link org.hibernate.metamodel.relational.SimpleValue},
	 * we return an Iterable over that single simple value.
	 *
	 * @return
	 */
	public Iterable<SimpleValue> getValues();

	/**
	 * @deprecated Use {@link #getValue()}.{@link Value#getTable() getTable()} instead; to be removed on completion of new metamodel code
	 * @return
	 */
	@Deprecated
	public TableSpecification getTable();

	public String getPropertyAccessorName();

	/**
	 *
	 * @return
	 */
	public boolean hasFormula();

	public boolean isAlternateUniqueKey();
	public boolean isNullable();
	public boolean[] getColumnUpdateability();
	public boolean[] getColumnInsertability();
	public boolean isSimpleValue();
	public boolean isEmbedded();
	public boolean isLazy();
}
