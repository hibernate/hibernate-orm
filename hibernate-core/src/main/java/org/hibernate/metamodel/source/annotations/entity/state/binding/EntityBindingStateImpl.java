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
package org.hibernate.metamodel.source.annotations.entity.state.binding;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.state.EntityBindingState;
import org.hibernate.metamodel.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;

/**
 * @author Hardy Ferentschik
 */
public class EntityBindingStateImpl implements EntityBindingState {
	private final boolean isRoot;
	private final InheritanceType inheritanceType;

	private String entityName;

	private Caching caching;

	private boolean mutable;
	private boolean explicitPolymorphism;
	private String whereFilter;
	private String rowId;

	private boolean dynamicUpdate;
	private boolean dynamicInsert;

	private int batchSize;
	private boolean selectBeforeUpdate;
	private OptimisticLockType optimisticLock;

	private Class<?> persisterClass;

	private boolean lazy;
	private String proxyInterfaceName;

	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	private Set<String> synchronizedTableNames;

	public EntityBindingStateImpl(ConfiguredClass configuredClass) {
		this.isRoot = configuredClass.isRoot();
		this.inheritanceType = configuredClass.getInheritanceType();
		this.synchronizedTableNames = new HashSet<String>();
		this.batchSize = -1;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setCaching(Caching caching) {
		this.caching = caching;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}

	public void setWhereFilter(String whereFilter) {
		this.whereFilter = whereFilter;
	}

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}

	public void setDynamicInsert(boolean dynamicInsert) {
		this.dynamicInsert = dynamicInsert;
	}

	public void setSelectBeforeUpdate(boolean selectBeforeUpdate) {
		this.selectBeforeUpdate = selectBeforeUpdate;
	}

	public void setOptimisticLock(OptimisticLockType optimisticLock) {
		this.optimisticLock = optimisticLock;
	}

	public void setPersisterClass(Class<?> persisterClass) {
		this.persisterClass = persisterClass;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setProxyInterfaceName(String proxyInterfaceName) {
		this.proxyInterfaceName = proxyInterfaceName;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void addSynchronizedTableName(String tableName) {
		synchronizedTableNames.add( tableName );
	}

	public void setCustomInsert(CustomSQL customInsert) {
		this.customInsert = customInsert;
	}

	public void setCustomUpdate(CustomSQL customUpdate) {
		this.customUpdate = customUpdate;
	}

	public void setCustomDelete(CustomSQL customDelete) {
		this.customDelete = customDelete;
	}

	@Override
	public boolean isRoot() {
		return isRoot;

	}

	@Override
	public InheritanceType getEntityInheritanceType() {
		return inheritanceType;
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		// not needed for annotations!? (HF)
		return null;
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
		return optimisticLock.ordinal();
	}

	@Override
	public Class getEntityPersisterClass() {
		return persisterClass;
	}

	@Override
	public Boolean isAbstract() {
		// no annotations equivalent
		return false;
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
	public Set<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}
}
