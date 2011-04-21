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

import org.hibernate.metamodel.binding.AbstractAttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLBag;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLDiscriminator;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLId;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLProperty;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLTimestamp;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLVersion;
import org.hibernate.metamodel.source.util.MappingHelper;

/**
 * @author Gail Badner
 */
public abstract class AbstractHbmAttributeDomainState implements AbstractAttributeBinding.DomainState {
	private final MappingDefaults defaults;
	private final Attribute attribute;
	private final HibernateTypeDescriptor hibernateTypeDescriptor;
	private final String accessorName;
	private final String cascade;
	private final boolean isOptimisticLockable;
	private final String nodeName;
	private final Map<String, MetaAttribute> metaAttributes;

	public AbstractHbmAttributeDomainState(MappingDefaults defaults,
										   Attribute attribute,
										   Map<String, MetaAttribute> entityMetaAttributes,
										   XMLId id) {
		this.defaults = defaults;
		this.attribute = attribute;
		this.hibernateTypeDescriptor = new HibernateTypeDescriptor();
		this.hibernateTypeDescriptor.setTypeName( id.getType() );
		this.accessorName =  HbmHelper.getPropertyAccessorName(
				id.getAccess(), isEmbedded(), defaults.getDefaultAccess()
		);
		this.nodeName = MappingHelper.getStringValue( id.getNode(), attribute.getName() );
		this.metaAttributes = HbmHelper.extractMetas( id.getMeta(), entityMetaAttributes );
		this.cascade = defaults.getDefaultCascade();
		this.isOptimisticLockable = true;
	}

	public AbstractHbmAttributeDomainState(MappingDefaults defaults,
										   Attribute attribute,
										   XMLDiscriminator discriminator) {

		this.defaults = defaults;
		this.attribute = attribute;
		this.hibernateTypeDescriptor = new HibernateTypeDescriptor();
		this.hibernateTypeDescriptor.setTypeName( discriminator.getType() == null ? "string" : discriminator.getType() );
		// the following does not apply to discriminators
		this.accessorName = null;
		this.nodeName = null;
		this.metaAttributes = null;
		this.cascade = null;
		this.isOptimisticLockable = true;
	}

	public AbstractHbmAttributeDomainState(MappingDefaults defaults,
										   Attribute attribute,
										   Map<String, MetaAttribute> entityMetaAttributes,
										   XMLVersion version) {
		this.defaults = defaults;
		this.attribute = attribute;
		this.hibernateTypeDescriptor = new HibernateTypeDescriptor();
		this.hibernateTypeDescriptor.setTypeName( version.getType() == null ? "integer" : version.getType() );

		// the following does not apply to discriminators
		this.accessorName =  HbmHelper.getPropertyAccessorName(
				version.getAccess(), isEmbedded(), defaults.getDefaultAccess()
		);
		this.nodeName = version.getNode();
		this.metaAttributes = HbmHelper.extractMetas( version.getMeta(), entityMetaAttributes );
		this.cascade = null;
		this.isOptimisticLockable = true;
	}

	public AbstractHbmAttributeDomainState(MappingDefaults defaults,
										   Attribute attribute,
										   Map<String, MetaAttribute> entityMetaAttributes,
										   XMLTimestamp timestamp) {
		this.defaults = defaults;
		this.attribute = attribute;
		this.hibernateTypeDescriptor = new HibernateTypeDescriptor();

		// Timestamp.getType() is not defined
		this.hibernateTypeDescriptor.setTypeName( "db".equals( timestamp.getSource() ) ? "dbtimestamp" : "timestamp" );

		// the following does not apply to discriminators
		this.accessorName =  HbmHelper.getPropertyAccessorName(
				timestamp.getAccess(), isEmbedded(), defaults.getDefaultAccess()
		);
		this.nodeName = timestamp.getNode();
		this.metaAttributes = HbmHelper.extractMetas( timestamp.getMeta(), entityMetaAttributes );
		this.cascade = null;
		this.isOptimisticLockable = true;
	}


	public AbstractHbmAttributeDomainState(MappingDefaults defaults,
										   Attribute attribute,
										   Map<String, MetaAttribute> entityMetaAttributes,
										   XMLProperty property) {
		this.defaults = defaults;
		this.attribute = attribute;
		this.hibernateTypeDescriptor = new HibernateTypeDescriptor();
		this.hibernateTypeDescriptor.setTypeName( property.getType() );
		this.accessorName =  HbmHelper.getPropertyAccessorName(
				property.getAccess(), isEmbedded(), defaults.getDefaultAccess()
		);
		this.nodeName = MappingHelper.getStringValue( property.getNode(), attribute.getName() );
		this.metaAttributes = HbmHelper.extractMetas( property.getMeta(), entityMetaAttributes );
		this.cascade = defaults.getDefaultCascade();
		this.isOptimisticLockable = MappingHelper.getBooleanValue( property.getOptimisticLock(), true );
	}

	public AbstractHbmAttributeDomainState(MappingDefaults defaults,
										   Attribute attribute,
										   Map<String, MetaAttribute> entityMetaAttributes,
										   XMLBag collection) {
		this.defaults = defaults;
		this.attribute = attribute;
		this.hibernateTypeDescriptor = new HibernateTypeDescriptor();
		// TODO: is collection.getCollectionType() correct here?
		this.hibernateTypeDescriptor.setTypeName( collection.getCollectionType() );
		this.accessorName =  HbmHelper.getPropertyAccessorName(
				collection.getAccess(), isEmbedded(), defaults.getDefaultAccess()
		);
		this.nodeName = MappingHelper.getStringValue( collection.getNode(), attribute.getName() );
		this.metaAttributes = HbmHelper.extractMetas( collection.getMeta(), entityMetaAttributes );
		this.cascade = defaults.getDefaultCascade();
		this.isOptimisticLockable = MappingHelper.getBooleanValue( collection.getOptimisticLock(), true );
	}

	protected final MappingDefaults getDefaults() {
		return defaults;
	}
	public final Attribute getAttribute() {
		return attribute;
	}
	public final HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}
	public final String getPropertyAccessorName() {
		return accessorName;
	}

	protected abstract boolean isEmbedded();

	public final boolean isAlternateUniqueKey() {
		//TODO: implement
		return false;
	}
	public final String getCascade() {
		return cascade;
	}
	public final boolean isOptimisticLockable() {
		return isOptimisticLockable;
	}
	public final String getNodeName() {
		return nodeName;
	}

	public final Map<String, MetaAttribute> getMetaAttributes(EntityBinding entityBinding) {
		return metaAttributes;
	}
}
