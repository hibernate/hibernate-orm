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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.Value;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeBinding implements AttributeBinding {
	public static interface DomainState {
		HibernateTypeDescriptor getHibernateTypeDescriptor();
		Attribute getAttribute();
		boolean isLazy();
		String getPropertyAccessorName();
		boolean isAlternateUniqueKey();
		String getCascade();
		boolean isOptimisticLockable();
		String getNodeName();
		Map<String, MetaAttribute> getMetaAttributes(EntityBinding entityBinding);
	}

	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final EntityBinding entityBinding;

	private Attribute attribute;
	private Value value;

	private boolean isLazy;
	private String propertyAccessorName;
	private boolean isAlternateUniqueKey;
	private String cascade;
	private boolean optimisticLockable;

	// DOM4J specific...
	private String nodeName;

	private Map<String, MetaAttribute> metaAttributes;

	protected AbstractAttributeBinding(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public void initialize(DomainState state) {
		hibernateTypeDescriptor.intialize( state.getHibernateTypeDescriptor() );
		attribute = state.getAttribute();
		isLazy = state.isLazy();
		propertyAccessorName = state.getPropertyAccessorName();
		isAlternateUniqueKey = state.isAlternateUniqueKey();
		cascade = state.getCascade();
		optimisticLockable = state.isOptimisticLockable();
		nodeName = state.getNodeName();
		metaAttributes = state.getMetaAttributes( entityBinding );
	}

	@Override
	public EntityBinding getEntityBinding() {
		return entityBinding;
	}

	@Override
	public Attribute getAttribute() {
		return attribute;
	}

	protected void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public Value getValue() {
		return value;
	}

	protected void setValue(Value value) {
		this.value = value;
	}

	@Override
	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	public String getCascade() {
		return cascade;
	}

	public boolean isOptimisticLockable() {
		return optimisticLockable;
	}

	public String getNodeName() {
		return nodeName;
	}

	@Override
	public Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	@Override
	public Iterable<SimpleValue> getValues() {
		return value == null
				? Collections.<SimpleValue>emptyList()
				: value instanceof Tuple
						? ( (Tuple) value ).values()
						: Collections.singletonList( (SimpleValue) value );
	}

	@Override
	public TableSpecification getTable() {
		return getValue().getTable();
	}

	@Override
	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	@Override
	public boolean hasFormula() {
		for ( SimpleValue simpleValue : getValues() ) {
			if ( simpleValue instanceof DerivedValue ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return isAlternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean alternateUniqueKey) {
		this.isAlternateUniqueKey = alternateUniqueKey;
	}

	@Override
	public boolean isNullable() {
		for ( SimpleValue simpleValue : getValues() ) {
			if ( simpleValue instanceof DerivedValue ) {
				return true;
			}
			Column column = (Column) simpleValue;
			if ( column.isNullable() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean[] getColumnInsertability() {
		List<Boolean> tmp = new ArrayList<Boolean>();
		for ( SimpleValue simpleValue : getValues() ) {
			tmp.add( ! ( simpleValue instanceof DerivedValue ) );
		}
		boolean[] rtn = new boolean[ tmp.size() ];
		int i = 0;
		for ( Boolean insertable : tmp ) {
			rtn[i++] = insertable.booleanValue();
		}
		return rtn;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		return getColumnInsertability();
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	protected void setLazy(boolean isLazy) {
		this.isLazy = isLazy;
	}
}
