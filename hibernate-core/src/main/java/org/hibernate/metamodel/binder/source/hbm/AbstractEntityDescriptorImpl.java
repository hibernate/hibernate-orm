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

import java.util.HashSet;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.metamodel.binder.Origin;
import org.hibernate.metamodel.binder.source.EntityDescriptor;
import org.hibernate.metamodel.binder.source.MetaAttributeContext;
import org.hibernate.metamodel.binder.source.UnifiedDescriptorObject;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlDeleteElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlInsertElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlUpdateElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSynchronizeElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLTuplizerElement;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * Convenience base class for handling commonality between the different type of {@link EntityDescriptor}
 * implementations.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public abstract class AbstractEntityDescriptorImpl implements EntityDescriptor {
	private final HbmBindingContext bindingContext;

	private final String entityName;
	private final EntityMode entityMode;

	private final String className;
	private final String proxyInterfaceName;

	private final Class<EntityPersister> entityPersisterClass;
	private final Class<EntityTuplizer> tuplizerClass;

	private final MetaAttributeContext metaAttributeContext;

	private final String superEntityName;
	private final InheritanceType entityInheritanceType;

	private final boolean lazy;

	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;

	private final int batchSize;
	private final boolean selectBeforeUpdate;

	private final Boolean isAbstract;

	private final String customLoaderName;
	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;

	private final Set<String> synchronizedTableNames;


	public AbstractEntityDescriptorImpl(
			EntityElement entityClazz,
			String superEntityName,
			InheritanceType inheritanceType,
			HbmBindingContext bindingContext) {

		this.bindingContext = bindingContext;

		this.superEntityName = superEntityName;
		this.entityName = bindingContext.determineEntityName( entityClazz );

		final String verbatimClassName = entityClazz.getName();
		this.entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;

		if ( this.entityMode == EntityMode.POJO ) {
			this.className = bindingContext.getClassName( verbatimClassName );
			this.proxyInterfaceName = entityClazz.getProxy();
		}
		else {
			this.className = null;
			this.proxyInterfaceName = null;
		}

		final String customTuplizerClassName = extractCustomTuplizerClassName( entityClazz, entityMode );
		this.tuplizerClass = customTuplizerClassName != null
				? bindingContext.<EntityTuplizer>locateClassByName( customTuplizerClassName )
				: null;
		this.entityPersisterClass = entityClazz.getPersister() == null
				? null
				: bindingContext.<EntityPersister>locateClassByName( entityClazz.getPersister() );

		this.entityInheritanceType = inheritanceType;


		this.metaAttributeContext = HbmHelper.extractMetaAttributeContext(
				entityClazz.getMeta(), true, bindingContext.getMetaAttributeContext()
		);

		// go ahead and set the lazy here, since pojo.proxy can override it.
		this.lazy = MappingHelper.getBooleanValue(
				entityClazz.isLazy(), bindingContext.getMappingDefaults().areAssociationsLazy()
		);

		this.dynamicUpdate = entityClazz.isDynamicUpdate();
		this.dynamicInsert = entityClazz.isDynamicInsert();
		this.batchSize = MappingHelper.getIntValue( entityClazz.getBatchSize(), 0 );
		this.selectBeforeUpdate = entityClazz.isSelectBeforeUpdate();

		this.customLoaderName = entityClazz.getLoader().getQueryRef();

		XMLSqlInsertElement sqlInsert = entityClazz.getSqlInsert();
		if ( sqlInsert != null ) {
			this.customInsert = HbmHelper.getCustomSql(
					sqlInsert.getValue(),
					sqlInsert.isCallable(),
					sqlInsert.getCheck().value()
			);
		}
		else {
			this.customInsert = null;
		}

		XMLSqlDeleteElement sqlDelete = entityClazz.getSqlDelete();
		if ( sqlDelete != null ) {
			this.customDelete = HbmHelper.getCustomSql(
					sqlDelete.getValue(),
					sqlDelete.isCallable(),
					sqlDelete.getCheck().value()
			);
		}
		else {
			this.customDelete = null;
		}

		XMLSqlUpdateElement sqlUpdate = entityClazz.getSqlUpdate();
		if ( sqlUpdate != null ) {
			this.customUpdate = HbmHelper.getCustomSql(
					sqlUpdate.getValue(),
					sqlUpdate.isCallable(),
					sqlUpdate.getCheck().value()
			);
		}
		else {
			this.customUpdate = null;
		}

		if ( entityClazz.getSynchronize() != null ) {
			this.synchronizedTableNames = new HashSet<String>( entityClazz.getSynchronize().size() );
			for ( XMLSynchronizeElement synchronize : entityClazz.getSynchronize() ) {
				this.synchronizedTableNames.add( synchronize.getTable() );
			}
		}
		else {
			this.synchronizedTableNames = null;
		}

		this.isAbstract = entityClazz.isAbstract();
	}

	protected boolean isRoot() {
		return entityInheritanceType == InheritanceType.NO_INHERITANCE;
	}

	private String extractCustomTuplizerClassName(EntityElement entityMapping, EntityMode entityMode) {
		if ( entityMapping.getTuplizer() == null ) {
			return null;
		}
		for ( XMLTuplizerElement tuplizerElement : entityMapping.getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getJpaEntityName() {
		return null;  // no such notion in hbm.xml files
	}

	@Override
	public EntityMode getEntityMode() {
		return entityMode;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getProxyInterfaceName() {
		return proxyInterfaceName;
	}

	@Override
	public Class<EntityPersister> getCustomEntityPersisterClass() {
		return entityPersisterClass;
	}

	@Override
	public Class<EntityTuplizer> getCustomEntityTuplizerClass() {
		return tuplizerClass;
	}

	@Override
	public String getSuperEntityName() {
		return superEntityName;
	}

	@Override
	public InheritanceType getEntityInheritanceType() {
		return entityInheritanceType;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	@Override
	public boolean isLazy() {
		return lazy;
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
	public Set<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	@Override
	public String getCustomLoaderName() {
		return customLoaderName;
	}

	@Override
	public UnifiedDescriptorObject getContainingDescriptor() {
		return null;
	}

	@Override
	public Origin getOrigin() {
		return bindingContext.getOrigin();
	}
}
