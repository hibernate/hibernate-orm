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

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.HbmBinder;
import org.hibernate.cfg.Mappings;
import org.hibernate.mapping.Fetchable;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.Metadata;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLManyToOneElement;
import org.hibernate.metamodel.source.util.DomHelper;
import org.hibernate.metamodel.source.util.MappingHelper;
import org.hibernate.tuple.component.Dom4jComponentTuplizer;

/**
 * @author Gail Badner
 */
public class HbmManyToOneAttributeDomainState
		extends AbstractHbmAttributeDomainState
		implements ManyToOneAttributeBinding.DomainState {

	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final XMLManyToOneElement manyToOne;
	private final String cascade;

	public HbmManyToOneAttributeDomainState(MappingDefaults defaults,
										   org.hibernate.metamodel.domain.Attribute attribute,
										   Map<String, MetaAttribute> entityMetaAttributes,
										   XMLManyToOneElement manyToOne) {
		super(
				defaults,
				attribute,
				manyToOne.getNode(),
				HbmHelper.extractMetas( manyToOne.getMeta(), entityMetaAttributes ),
				HbmHelper.getPropertyAccessorName( manyToOne.getAccess(), manyToOne.isEmbedXml(), defaults.getDefaultAccess() ),
				manyToOne.isOptimisticLock()
		);

		this.hibernateTypeDescriptor.setTypeName( getReferencedEntityName() );
		this.manyToOne = manyToOne;
		this.cascade = MappingHelper.getStringValue( manyToOne.getCascade(), defaults.getDefaultCascade() );
	}

	// TODO: is this needed???
	protected boolean isEmbedded() {
		return MappingHelper.getBooleanValue( manyToOne.isEmbedXml(), true );
	}

	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	// same as for plural attributes...
	public FetchMode getFetchMode() {
		FetchMode fetchMode;
		if ( manyToOne.getFetch() != null ) {
			fetchMode = "join".equals( manyToOne.getFetch() ) ? FetchMode.JOIN : FetchMode.SELECT;
		}
		else {
			String jfNodeValue = ( manyToOne.getOuterJoin().value() == null ? "auto" : manyToOne.getOuterJoin().value() );
			if ( "auto".equals( jfNodeValue ) ) {
				fetchMode = FetchMode.DEFAULT;
			}
			else if ( "true".equals( jfNodeValue ) ) {
				fetchMode = FetchMode.JOIN;
			}
			else {
				fetchMode = FetchMode.SELECT;
			}
		}
		return fetchMode;
	}

	public boolean isLazy() {
		return manyToOne.getLazy() == null ||
				isUnwrapProxy() ||
				manyToOne.getLazy().equals( "proxy" );
		//TODO: better to degrade to lazy="false" if uninstrumented
	}

	public boolean isUnwrapProxy() {
		return "no-proxy".equals( manyToOne.getLazy() );
	}

	public String getReferencedAttributeName() {
		return manyToOne.getPropertyRef();
	}

	public String getReferencedEntityName() {
		String entityName = manyToOne.getEntityName();
		return entityName == null ?
				HbmHelper.getClassName( manyToOne.getClazz(), getDefaults().getPackageName() ) :
				entityName.intern();
	}

	public String getCascade() {
		return MappingHelper.getStringValue( manyToOne.getCascade(), getDefaults().getDefaultCascade() );
	}

	public boolean ignoreNotFound() {
		return "ignore".equals( manyToOne.getNotFound() );
	}

	/*
	void junk() {
		if( getReferencedPropertyName() != null && ! ignoreNotFound() ) {
				mappings.addSecondPass( new ManyToOneSecondPass(manyToOne) );
		}
	}
	*/

	public boolean isInsertable() {
		return MappingHelper.getBooleanValue( manyToOne.isInsert(), true );
	}

	public boolean isUpdateable() {
		return MappingHelper.getBooleanValue( manyToOne.isUnique(), true );
	}

	public String getForeignkeyName() {
		return manyToOne.getForeignKey();
	}

	public boolean isKeyCasadeDeleteEnabled() {
		//TODO: implement
		return false;
	}
	public String getUnsavedValue() {
		//TODO: implement
		return null;
	}
}
