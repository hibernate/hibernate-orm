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
package org.hibernate.metamodel.binder;

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.metamodel.binder.view.EntityView;
import org.hibernate.metamodel.binder.view.TableView;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.spi.BindingContext;

/**
 * @author Steve Ebersole
 */
public class EntityBinder {
	private final BindingContext bindingContext;

	public EntityBinder(BindingContext bindingContext) {
		this.bindingContext = bindingContext;
	}

	public EntityBinding createEntityBinding(EntityView entityView) {
		final EntityBinding entityBinding = new EntityBinding();

		// todo : Entity will need both entityName and className to be effective
		final Entity entity = new Entity( entityView.getEntityName(), entityView.getSuperType(), bindingContext.makeJavaType( entityView.getClassName() ) );
		entityBinding.setEntity( entity );

		final TableView baseTableView = entityView.getBaseTable();

		final String schemaName = baseTableView.getExplicitSchemaName() == null
				? bindingContext.getMappingDefaults().getSchemaName()
				: baseTableView.getExplicitSchemaName();
		final String catalogName = baseTableView.getExplicitCatalogName() == null
				? bindingContext.getMappingDefaults().getCatalogName()
				: baseTableView.getExplicitCatalogName();
		final Schema.Name fullSchemaName = new Schema.Name( schemaName, catalogName );
		final Schema schema = bindingContext.getMetadataImplementor().getDatabase().getSchema( fullSchemaName );
		final Identifier tableName = Identifier.toIdentifier( baseTableView.getTableName() );
		final Table baseTable = schema.locateOrCreateTable( tableName );
		entityBinding.setBaseTable( baseTable );

		// inheritance
		if ( entityView.getEntityInheritanceType() != InheritanceType.NO_INHERITANCE ) {
			// if there is any inheritance strategy, there has to be a super type.
			if ( entityView.getSuperType() == null ) {
				throw new MappingException( "Encountered inheritance strategy, but no super type", entityView.getOrigin() );
			}
		}
		entityBinding.setRoot( entityView.isRoot() );
		entityBinding.setInheritanceType( entityView.getEntityInheritanceType() );

		entityBinding.setJpaEntityName( entityView.getJpaEntityName() );
		entityBinding.setEntityMode( entityView.getEntityMode() );

		if ( entityView.getEntityMode() == EntityMode.POJO ) {
			if ( entityView.getProxyInterfaceName() != null ) {
				entityBinding.setProxyInterfaceType( bindingContext.makeJavaType( entityView.getProxyInterfaceName() ) );
				entityBinding.setLazy( true );
			}
			else if ( entityView.isLazy() ) {
				entityBinding.setProxyInterfaceType( entity.getJavaType() );
				entityBinding.setLazy( true );
			}
		}
		else {
			entityBinding.setProxyInterfaceType( new JavaType( Map.class ) );
			entityBinding.setLazy( entityView.isLazy() );
		}

		entityBinding.setCustomEntityTuplizerClass( entityView.getCustomEntityTuplizerClass() );
		entityBinding.setCustomEntityPersisterClass( entityView.getCustomEntityPersisterClass() );

		entityBinding.setCaching( entityView.getCaching() );
		entityBinding.setMetaAttributeContext( entityView.getMetaAttributeContext() );

		entityBinding.setMutable( entityView.isMutable() );
		entityBinding.setExplicitPolymorphism( entityView.isExplicitPolymorphism() );
		entityBinding.setWhereFilter( entityView.getWhereFilter() );
		entityBinding.setRowId( entityView.getRowId() );
		entityBinding.setDynamicUpdate( entityView.isDynamicUpdate() );
		entityBinding.setDynamicInsert( entityView.isDynamicInsert() );
		entityBinding.setBatchSize( entityView.getBatchSize() );
		entityBinding.setSelectBeforeUpdate( entityView.isSelectBeforeUpdate() );
		entityBinding.setOptimisticLockMode( entityView.getOptimisticLockMode() );
		entityBinding.setAbstract( entityView.isAbstract() );

		entityBinding.setCustomLoaderName( entityView.getCustomLoaderName() );
		entityBinding.setCustomInsert( entityView.getCustomInsert() );
		entityBinding.setCustomUpdate( entityView.getCustomUpdate() );
		entityBinding.setCustomDelete( entityView.getCustomDelete() );

		if ( entityView.getSynchronizedTableNames() != null ) {
			entityBinding.addSynchronizedTableNames( entityView.getSynchronizedTableNames() );
		}

		return entityBinding;
	}
}
