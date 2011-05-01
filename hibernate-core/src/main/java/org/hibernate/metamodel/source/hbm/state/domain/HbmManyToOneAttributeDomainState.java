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

import org.hibernate.FetchMode;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLManyToOneElement;
import org.hibernate.metamodel.source.util.MappingHelper;

/**
 * @author Gail Badner
 */
public class HbmManyToOneAttributeDomainState
		extends AbstractHbmAttributeDomainState
		implements ManyToOneAttributeBinding.DomainState {

	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final FetchMode fetchMode;
	private final boolean isUnwrapProxy;
	private final boolean isLazy;
	private final String cascade;
	private final boolean isEmbedded;
	private final String referencedPropertyName;
	private final String referencedEntityName;
	private final boolean ignoreNotFound;
	private final boolean isInsertable;
	private final boolean isUpdateable;

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
		fetchMode = getFetchMode( manyToOne );
		isUnwrapProxy = manyToOne.getLazy() != null && "no-proxy".equals( manyToOne.getLazy().value() );
		//TODO: better to degrade to lazy="false" if uninstrumented
		isLazy =  manyToOne.getLazy() == null ||
				isUnwrapProxy ||
				"proxy".equals( manyToOne.getLazy().value() );
		cascade = MappingHelper.getStringValue( manyToOne.getCascade(), defaults.getDefaultCascade() );
		isEmbedded = manyToOne.isEmbedXml();
		hibernateTypeDescriptor.setTypeName( getReferencedEntityName() );
		referencedPropertyName = manyToOne.getPropertyRef();
		referencedEntityName = (
				manyToOne.getEntityName() == null ?
				HbmHelper.getClassName( manyToOne.getClazz(), getDefaults().getPackageName() ) :
				manyToOne.getEntityName().intern()
		);
		ignoreNotFound = "ignore".equals( manyToOne.getNotFound().value() );
		isInsertable = manyToOne.isInsert();
		isUpdateable = manyToOne.isUpdate();
	}

	// TODO: is this needed???
	protected boolean isEmbedded() {
		return isEmbedded;
	}

	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	// same as for plural attributes...
	private static FetchMode getFetchMode(XMLManyToOneElement manyToOne) {
		FetchMode fetchMode;
		if ( manyToOne.getFetch() != null ) {
			fetchMode = "join".equals( manyToOne.getFetch().value() ) ? FetchMode.JOIN : FetchMode.SELECT;
		}
		else {
			String jfNodeValue = ( manyToOne.getOuterJoin() == null ? "auto" : manyToOne.getOuterJoin().value() );
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

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public boolean isUnwrapProxy() {
		return isUnwrapProxy;
	}

	public String getReferencedAttributeName() {
		return referencedPropertyName;
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public String getCascade() {
		return cascade;
	}

	public boolean ignoreNotFound() {
		return ignoreNotFound;
	}

	public boolean isInsertable() {
		return isInsertable;
	}

	public boolean isUpdateable() {
		return isUpdateable;
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
