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
package org.hibernate.metamodel.source.hbm.state.domain;

import java.util.Map;

import org.dom4j.Element;

import org.hibernate.mapping.MetaAttribute;
import org.hibernate.metamodel.binding.AbstractAttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * @author Gail Badner
 */
public abstract class AbstractHbmAttributeDomainState implements AbstractAttributeBinding.DomainState {
	private final MappingDefaults defaults;
	private final Element element;
	private final Attribute attribute;
	public AbstractHbmAttributeDomainState(MappingDefaults defaults,
										Element element,
										Attribute attribute) {
		this.defaults = defaults;
		this.element = element;
		this.attribute = attribute;
	}

	protected final MappingDefaults getDefaults() {
		return defaults;
	}
	protected final Element getElement() {
		return element;
	}

	public final HibernateTypeDescriptor getHibernateTypeDescriptor() {
		HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
		hibernateTypeDescriptor.setTypeName( DomHelper.extractAttributeValue( element, "type", null ) );
		return hibernateTypeDescriptor;
	}
	public final Attribute getAttribute() {
		return attribute;
	}
	public final String getPropertyAccessorName() {
		return HbmHelper.getPropertyAccessorName( element, isEmbedded(), defaults.getDefaultAccess() );
	}

	protected abstract boolean isEmbedded();

	public final boolean isAlternateUniqueKey() {
		//TODO: implement
		return false;
	}
	public final String getCascade() {
		return DomHelper.extractAttributeValue( element, "cascade", defaults.getDefaultCascade() );
	}
	public final boolean isOptimisticLockable() {
		return DomHelper.extractBooleanAttributeValue( element, "optimistic-lock", true );
	}
	public final String getNodeName() {
		return DomHelper.extractAttributeValue( element, "node", attribute.getName() );
	}

	public final Map<String, MetaAttribute> getMetaAttributes(EntityBinding entityBinding) {
		//TODO: implement
		return HbmHelper.extractMetas( element, entityBinding.getMetaAttributes() );
	}
}
