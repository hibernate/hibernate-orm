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
package org.hibernate.metamodel.source.hbm.state.binding;

import java.util.Map;

import org.hibernate.FetchMode;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLManyToOneElement;
import org.hibernate.metamodel.source.util.MappingHelper;
import org.hibernate.metamodel.state.binding.ManyToOneAttributeBindingState;

/**
 * @author Gail Badner
 */
public class HbmManyToOneAttributeBindingState
		extends AbstractHbmAttributeBindingState
		implements ManyToOneAttributeBindingState {

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

	public HbmManyToOneAttributeBindingState(
			String ownerClassName,
			MappingDefaults defaults,
			Map<String, MetaAttribute> entityMetaAttributes,
			XMLManyToOneElement manyToOne) {
		super(
				ownerClassName,
				manyToOne.getName(),
				defaults,
				manyToOne.getNode(),
				HbmHelper.extractMetas( manyToOne.getMeta(), entityMetaAttributes ),
				HbmHelper.getPropertyAccessorName(
						manyToOne.getAccess(), manyToOne.isEmbedXml(), defaults.getDefaultAccess()
				),
				manyToOne.isOptimisticLock()
		);
		fetchMode = getFetchMode( manyToOne );
		isUnwrapProxy = manyToOne.getLazy() != null && "no-proxy".equals( manyToOne.getLazy().value() );
		//TODO: better to degrade to lazy="false" if uninstrumented
		isLazy = manyToOne.getLazy() == null ||
				isUnwrapProxy ||
				"proxy".equals( manyToOne.getLazy().value() );
		cascade = MappingHelper.getStringValue( manyToOne.getCascade(), defaults.getDefaultCascade() );
		isEmbedded = manyToOne.isEmbedXml();
		referencedEntityName = getReferencedEntityName( ownerClassName, manyToOne, defaults );
		referencedPropertyName = manyToOne.getPropertyRef();
		ignoreNotFound = "ignore".equals( manyToOne.getNotFound().value() );
		isInsertable = manyToOne.isInsert();
		isUpdateable = manyToOne.isUpdate();
	}

	// TODO: is this needed???
	protected boolean isEmbedded() {
		return isEmbedded;
	}

	private static String getReferencedEntityName(String ownerClassName, XMLManyToOneElement manyToOne, MappingDefaults defaults) {
		String referencedEntityName;
		if ( manyToOne.getEntityName() != null ) {
			referencedEntityName = manyToOne.getEntityName();
		}
		else if ( manyToOne.getClazz() != null ) {
			referencedEntityName = HbmHelper.getClassName( manyToOne.getClazz(), defaults.getPackageName() );
		}
		else {
			Class ownerClazz = MappingHelper.classForName( ownerClassName, defaults.getServiceRegistry() );
			referencedEntityName = ReflectHelper.reflectedPropertyClass( ownerClazz, manyToOne.getName() ).getName();
		}
		return referencedEntityName;
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

	public String getTypeName() {
		return referencedEntityName;
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

	public boolean isKeyCascadeDeleteEnabled() {
		//TODO: implement
		return false;
	}

	public String getUnsavedValue() {
		//TODO: implement
		return null;
	}
}
