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
package org.hibernate.metamodel.binder.source.hbm;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.binder.MappingException;
import org.hibernate.metamodel.binder.source.RootEntityDescriptor;
import org.hibernate.metamodel.binder.source.TableDescriptor;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLCacheElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;

/**
 * Unified descriptor for root entity (no inheritance strategy).
 *
 * @author Steve Ebersole
 */
public class RootEntityDescriptorImpl extends AbstractEntityDescriptorImpl implements RootEntityDescriptor {
	private final boolean mutable;
	private final boolean explicitPolymorphism;
	private final String whereFilter;
	private final String rowId;
	private final Caching caching;
	private final OptimisticLockStyle optimisticLockStyle;

	private final TableDescriptor baseTableDescriptor;

	public RootEntityDescriptorImpl(EntityElement entityClazz, HbmBindingContext bindingContext) {
		super( entityClazz, null, InheritanceType.NO_INHERITANCE, bindingContext );

		// the mapping has to be <class/>
		final XMLHibernateMapping.XMLClass xmlClass = (XMLHibernateMapping.XMLClass) entityClazz;

		this.mutable = xmlClass.isMutable();
		this.explicitPolymorphism = "explicit".equals( xmlClass.getPolymorphism() );
		this.whereFilter = xmlClass.getWhere();
		this.rowId = xmlClass.getRowid();
		this.caching = interpretCaching( xmlClass, getEntityName() );
		this.optimisticLockStyle = interpretOptimisticLockStyle( xmlClass, bindingContext );

		this.baseTableDescriptor = new TableDescriptorImpl(
				xmlClass.getSchema(),
				xmlClass.getCatalog(),
				xmlClass.getTable(),
				this,
				bindingContext
		);
	}

	private static Caching interpretCaching(XMLHibernateMapping.XMLClass xmlClass, String entityName) {
		final XMLCacheElement cache = xmlClass.getCache();
		if ( cache == null ) {
			return null;
		}
		final String region = cache.getRegion() != null ? cache.getRegion() : entityName;
		final AccessType accessType = Enum.valueOf( AccessType.class, cache.getUsage() );
		final boolean cacheLazyProps = !"non-lazy".equals( cache.getInclude() );
		return new Caching( region, accessType, cacheLazyProps );
	}

	private static OptimisticLockStyle interpretOptimisticLockStyle(
			XMLHibernateMapping.XMLClass entityClazz,
			HbmBindingContext bindingContext) {
		final String optimisticLockModeString = MappingHelper.getStringValue( entityClazz.getOptimisticLock(), "version" );
		try {
			return OptimisticLockStyle.valueOf( optimisticLockModeString.toUpperCase() );
		}
		catch (Exception e) {
			throw new MappingException(
					"Unknown optimistic-lock value : " + optimisticLockModeString,
					bindingContext.getOrigin()
			);
		}
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
	public Caching getCaching() {
		return caching;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public TableDescriptor getBaseTable() {
		return baseTableDescriptor;
	}
}
