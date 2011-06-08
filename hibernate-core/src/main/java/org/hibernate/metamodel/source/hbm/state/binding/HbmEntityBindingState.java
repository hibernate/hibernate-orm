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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.state.EntityBindingState;
import org.hibernate.metamodel.source.hbm.HbmBindingContext;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.util.MappingHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLCacheElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlDeleteElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlInsertElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlUpdateElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSynchronizeElement;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;

/**
 * @author Gail Badner
 */
public class HbmEntityBindingState implements EntityBindingState {
	private final boolean isRoot;
	private final InheritanceType entityInheritanceType;
	private final Caching caching;
	private final MetaAttributeContext metaAttributeContext;
	private final String proxyInterfaceName;

	private final boolean lazy;
	private final boolean mutable;
	private final boolean explicitPolymorphism;
	private final String whereFilter;
	private final String rowId;

	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;

	private final int batchSize;
	private final boolean selectBeforeUpdate;
	private final int optimisticLockMode;

	private final Class entityPersisterClass;
	private final Boolean isAbstract;

	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;

	private final List<String> synchronizedTableNames;

	public HbmEntityBindingState(
			boolean isRoot,
			InheritanceType inheritanceType,
			HbmBindingContext bindingContext,
			XMLHibernateMapping.XMLClass entityClazz) {
		this.isRoot = isRoot;
		this.entityInheritanceType = inheritanceType;

		this.caching = createCaching( entityClazz,  bindingContext.extractEntityName( entityClazz ) );

		metaAttributeContext = HbmHelper.extractMetaAttributeContext(
				entityClazz.getMeta(), true, bindingContext.getMetaAttributeContext()
		);

		// go ahead and set the lazy here, since pojo.proxy can override it.
		lazy = MappingHelper.getBooleanValue(
				entityClazz.isLazy(), bindingContext.getMappingDefaults().isDefaultLazy()
		);
		mutable = entityClazz.isMutable();

		explicitPolymorphism = "explicit".equals( entityClazz.getPolymorphism() );
		whereFilter = entityClazz.getWhere();
		rowId = entityClazz.getRowid();
		proxyInterfaceName = entityClazz.getProxy();
		dynamicUpdate = entityClazz.isDynamicUpdate();
		dynamicInsert = entityClazz.isDynamicInsert();
		batchSize = MappingHelper.getIntValue( entityClazz.getBatchSize(), 0 );
		selectBeforeUpdate = entityClazz.isSelectBeforeUpdate();
		optimisticLockMode = getOptimisticLockMode();

		// PERSISTER
		entityPersisterClass =
				entityClazz.getPersister() == null ?
						null :
						MappingHelper.classForName( entityClazz.getPersister(), bindingContext.getServiceRegistry() );

		// CUSTOM SQL
		XMLSqlInsertElement sqlInsert = entityClazz.getSqlInsert();
		if ( sqlInsert != null ) {
			customInsert = HbmHelper.getCustomSql(
					sqlInsert.getValue(),
					sqlInsert.isCallable(),
					sqlInsert.getCheck().value()
			);
		}
		else {
			customInsert = null;
		}

		XMLSqlDeleteElement sqlDelete = entityClazz.getSqlDelete();
		if ( sqlDelete != null ) {
			customDelete = HbmHelper.getCustomSql(
					sqlDelete.getValue(),
					sqlDelete.isCallable(),
					sqlDelete.getCheck().value()
			);
		}
		else {
			customDelete = null;
		}

		XMLSqlUpdateElement sqlUpdate = entityClazz.getSqlUpdate();
		if ( sqlUpdate != null ) {
			customUpdate = HbmHelper.getCustomSql(
					sqlUpdate.getValue(),
					sqlUpdate.isCallable(),
					sqlUpdate.getCheck().value()
			);
		}
		else {
			customUpdate = null;
		}

		if ( entityClazz.getSynchronize() != null ) {
			synchronizedTableNames = new ArrayList<String>( entityClazz.getSynchronize().size() );
			for ( XMLSynchronizeElement synchronize : entityClazz.getSynchronize() ) {
				synchronizedTableNames.add( synchronize.getTable() );
			}
		}
		else {
			synchronizedTableNames = null;
		}
		isAbstract = entityClazz.isAbstract();
	}

	private static Caching createCaching(XMLHibernateMapping.XMLClass entityClazz, String entityName) {
		XMLCacheElement cache = entityClazz.getCache();
		if ( cache == null ) {
			return null;
		}
		final String region = cache.getRegion() != null ? cache.getRegion() : entityName;
		final AccessType accessType = Enum.valueOf( AccessType.class, cache.getUsage() );
		final boolean cacheLazyProps = !"non-lazy".equals( cache.getInclude() );
		return new Caching( region, accessType, cacheLazyProps );
	}

	private static int createOptimisticLockMode(XMLHibernateMapping.XMLClass entityClazz) {
		String optimisticLockModeString = MappingHelper.getStringValue( entityClazz.getOptimisticLock(), "version" );
		int optimisticLockMode;
		if ( "version".equals( optimisticLockModeString ) ) {
			optimisticLockMode = Versioning.OPTIMISTIC_LOCK_VERSION;
		}
		else if ( "dirty".equals( optimisticLockModeString ) ) {
			optimisticLockMode = Versioning.OPTIMISTIC_LOCK_DIRTY;
		}
		else if ( "all".equals( optimisticLockModeString ) ) {
			optimisticLockMode = Versioning.OPTIMISTIC_LOCK_ALL;
		}
		else if ( "none".equals( optimisticLockModeString ) ) {
			optimisticLockMode = Versioning.OPTIMISTIC_LOCK_NONE;
		}
		else {
			throw new MappingException( "Unsupported optimistic-lock style: " + optimisticLockModeString );
		}
		return optimisticLockMode;
	}

	@Override
	public boolean isRoot() {
		return isRoot;

	}

	@Override
	public InheritanceType getEntityInheritanceType() {
		return entityInheritanceType;
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	@Override
	public String getProxyInterfaceName() {
		return proxyInterfaceName;
	}

	@Override
	public boolean isLazy() {
		return lazy;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	@Override
	public String getWhereFilter() {
		return whereFilter;
	}

	@Override
	public String getRowId() {
		return rowId;
	}

	@Override
	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	@Override
	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	@Override
	public int getOptimisticLockMode() {
		return optimisticLockMode;
	}

	@Override
	public Class getEntityPersisterClass() {
		return entityPersisterClass;
	}

	@Override
	public Boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	@Override
	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	@Override
	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	@Override
	public List<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}
}
