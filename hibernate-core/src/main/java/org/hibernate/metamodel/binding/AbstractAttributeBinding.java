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

import org.dom4j.Element;

import org.hibernate.mapping.MetaAttribute;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.Value;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeBinding implements AttributeBinding {
	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final EntityBinding entityBinding;

	private Attribute attribute;
	private Value value;

	private boolean isLazy;
	private String propertyAccessorName;
	private boolean alternateUniqueKey;
	private String cascade;
	private boolean optimisticLockable;

	// DOM4J specific...
	private String nodeName;

	private Map<String, MetaAttribute> metaAttributes;

	protected AbstractAttributeBinding(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public void fromHbmXml(MappingDefaults defaults, Element element, Attribute attribute) {
		this.attribute = attribute;
		hibernateTypeDescriptor.setTypeName( DomHelper.extractAttributeValue( element, "type", null ) );

		metaAttributes = HbmHelper.extractMetas( element, entityBinding.getMetaAttributes() );
		nodeName = DomHelper.extractAttributeValue( element, "node", attribute.getName() );
		isLazy = DomHelper.extractBooleanAttributeValue( element, "lazy", isLazyDefault( defaults ) );
		propertyAccessorName =  (
				DomHelper.extractAttributeValue(
						element,
						"access",
						isEmbedded() ? "embedded" : defaults.getDefaultAccess()
				)
		);
		cascade = DomHelper.extractAttributeValue( element, "cascade", defaults.getDefaultCascade() );
		optimisticLockable = DomHelper.extractBooleanAttributeValue( element, "optimistic-lock", true );
	}

	@Override
	public EntityBinding getEntityBinding() {
		return entityBinding;
	}

	@Override
	public Attribute getAttribute() {
		return attribute;
	}

	@Override
	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public Value getValue() {
		return value;
	}

	@Override
	public void setValue(Value value) {
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
		return alternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean alternateUniqueKey) {
		this.alternateUniqueKey = alternateUniqueKey;
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

	protected abstract boolean isLazyDefault(MappingDefaults defaults);

	protected void setLazy(boolean isLazy) {
		this.isLazy = isLazy;
	}
}
