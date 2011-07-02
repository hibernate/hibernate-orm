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

import org.hibernate.EntityMode;
import org.hibernate.metamodel.binder.Origin;
import org.hibernate.metamodel.binder.source.BindingContext;
import org.hibernate.metamodel.binder.source.EntityDescriptor;
import org.hibernate.metamodel.binder.source.MetaAttributeContext;
import org.hibernate.metamodel.binder.source.UnifiedDescriptorObject;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * @author Hardy Ferentschik
 */
public abstract class AbstractEntityDescriptorImpl implements EntityDescriptor {
	private final BindingContext bindingContext;

	private final String className;
	private final String superEntityName;
	private final InheritanceType inheritanceType;

	private String jpaEntityName;

	private boolean lazy;
	private String proxyInterfaceName;

	private Class<? extends EntityPersister> persisterClass;
	private Class<? extends EntityTuplizer> tuplizerClass;

	private boolean dynamicUpdate;
	private boolean dynamicInsert;

	private int batchSize = -1;
	private boolean selectBeforeUpdate;

	private String customLoaderName;
	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	private Set<String> synchronizedTableNames = new HashSet<String>();

	public AbstractEntityDescriptorImpl(
			EntityClass entityClass,
			String superEntityName,
			BindingContext bindingContext) {
		this.bindingContext = bindingContext;

		this.className = entityClass.getName();
		this.superEntityName = superEntityName;
		this.inheritanceType = entityClass.getInheritanceType();
	}

	@Override
	public String getEntityName() {
		return className;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	public void setJpaEntityName(String entityName) {
		this.jpaEntityName = entityName;
	}

	@Override
	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public String getSuperEntityName() {
		return superEntityName;
	}

	@Override
	public InheritanceType getEntityInheritanceType() {
		return inheritanceType;
	}

	@Override
	public Boolean isAbstract() {
		// no annotations equivalent
		return Boolean.FALSE;
	}

	@Override
	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	@Override
	public String getProxyInterfaceName() {
		return proxyInterfaceName;
	}

	public void setProxyInterfaceName(String proxyInterfaceName) {
		this.proxyInterfaceName = proxyInterfaceName;
	}

	@Override
	public Class<? extends EntityPersister> getCustomEntityPersisterClass() {
		return persisterClass;
	}

	public void setPersisterClass(Class<? extends EntityPersister> persisterClass) {
		this.persisterClass = persisterClass;
	}

	@Override
	public Class<? extends EntityTuplizer> getCustomEntityTuplizerClass() {
		return tuplizerClass;
	}

	public void setTuplizerClass(Class<? extends EntityTuplizer> tuplizerClass) {
		this.tuplizerClass = tuplizerClass;
	}

	@Override
	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}

	@Override
	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public void setDynamicInsert(boolean dynamicInsert) {
		this.dynamicInsert = dynamicInsert;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public void setSelectBeforeUpdate(boolean selectBeforeUpdate) {
		this.selectBeforeUpdate = selectBeforeUpdate;
	}

	@Override
	public String getCustomLoaderName() {
		return customLoaderName;
	}

	public void setCustomLoaderName(String customLoaderName) {
		this.customLoaderName = customLoaderName;
	}

	@Override
	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public void setCustomInsert(CustomSQL customInsert) {
		this.customInsert = customInsert;
	}

	@Override
	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public void setCustomUpdate(CustomSQL customUpdate) {
		this.customUpdate = customUpdate;
	}

	@Override
	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public void setCustomDelete(CustomSQL customDelete) {
		this.customDelete = customDelete;
	}

	@Override
	public Set<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	public void addSynchronizedTableName(String tableName) {
		synchronizedTableNames.add( tableName );
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		// not needed for annotations!? (HF)
		// probably not; this is a tools/generation thing (SE)
		return null;
	}

	@Override
	public Origin getOrigin() {
		// (steve) - not sure how to best handle this.  Origin should essentially name the class file from which
		// this information came
		return null;
	}

	@Override
	public UnifiedDescriptorObject getContainingDescriptor() {
		// probably makes most sense as none for annotations.
		return this;
	}
}
