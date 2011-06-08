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

import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.binding.CascadeType;
import org.hibernate.metamodel.binding.state.ManyToOneAttributeBindingState;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.util.MappingHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLManyToOneElement;
import org.hibernate.metamodel.source.spi.BindingContext;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;

/**
 * @author Gail Badner
 */
public class HbmManyToOneAttributeBindingState
		extends AbstractHbmAttributeBindingState
		implements ManyToOneAttributeBindingState {

	private final FetchMode fetchMode;
	private final boolean isUnwrapProxy;
	private final boolean isLazy;
	private final Set<CascadeType> cascadeTypes;
	private final boolean isEmbedded;
	private final String referencedPropertyName;
	private final String referencedEntityName;
	private final boolean ignoreNotFound;
	private final boolean isInsertable;
	private final boolean isUpdateable;

	public HbmManyToOneAttributeBindingState(
			String ownerClassName,
			BindingContext bindingContext,
			MetaAttributeContext parentMetaAttributeContext,
			XMLManyToOneElement manyToOne) {
		super(
				ownerClassName,
				manyToOne.getName(),
				bindingContext,
				manyToOne.getNode(),
				HbmHelper.extractMetaAttributeContext( manyToOne.getMeta(), parentMetaAttributeContext ),
				HbmHelper.getPropertyAccessorName(
						manyToOne.getAccess(),
						manyToOne.isEmbedXml(),
						bindingContext.getMappingDefaults().getPropertyAccessorName()
				),
				manyToOne.isOptimisticLock()
		);
		fetchMode = getFetchMode( manyToOne );
		isUnwrapProxy = manyToOne.getLazy() != null && "no-proxy".equals( manyToOne.getLazy().value() );
		//TODO: better to degrade to lazy="false" if uninstrumented
		isLazy = manyToOne.getLazy() == null ||
				isUnwrapProxy ||
				"proxy".equals( manyToOne.getLazy().value() );
		cascadeTypes = determineCascadeTypes( manyToOne.getCascade() );
		isEmbedded = manyToOne.isEmbedXml();
		referencedEntityName = getReferencedEntityName( ownerClassName, manyToOne, bindingContext );
		referencedPropertyName = manyToOne.getPropertyRef();
		ignoreNotFound = "ignore".equals( manyToOne.getNotFound().value() );
		isInsertable = manyToOne.isInsert();
		isUpdateable = manyToOne.isUpdate();
	}

	// TODO: is this needed???
	protected boolean isEmbedded() {
		return isEmbedded;
	}

	private static String getReferencedEntityName(
			String ownerClassName,
			XMLManyToOneElement manyToOne,
			BindingContext bindingContext) {
		String referencedEntityName;
		if ( manyToOne.getEntityName() != null ) {
			referencedEntityName = manyToOne.getEntityName();
		}
		else if ( manyToOne.getClazz() != null ) {
			referencedEntityName = HbmHelper.getClassName(
					manyToOne.getClazz(), bindingContext.getMappingDefaults().getPackageName()
			);
		}
		else {
			Class ownerClazz = MappingHelper.classForName( ownerClassName, bindingContext.getServiceRegistry() );
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

	public Set<CascadeType> getCascadeTypes() {
		return cascadeTypes;
	}

	public boolean ignoreNotFound() {
		return ignoreNotFound;
	}

	public boolean isInsertable() {
		return isInsertable;
	}

	public boolean isUpdatable() {
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
