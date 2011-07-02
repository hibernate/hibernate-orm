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
import org.hibernate.metamodel.binder.source.BindingContext;
import org.hibernate.metamodel.binder.source.DiscriminatorSubClassEntityDescriptor;
import org.hibernate.metamodel.binder.source.EntityDescriptor;
import org.hibernate.metamodel.binder.source.JoinedSubClassEntityDescriptor;
import org.hibernate.metamodel.binder.source.RootEntityDescriptor;
import org.hibernate.metamodel.binder.source.UnionSubClassEntityDescriptor;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.JavaType;

/**
 * @author Steve Ebersole
 */
public class EntityBinder {
	private final BindingContext bindingContext;

	public EntityBinder(BindingContext bindingContext) {
		this.bindingContext = bindingContext;
	}

	public EntityBinding createEntityBinding(EntityDescriptor entityDescriptor) {
		final InheritanceType inheritanceType = entityDescriptor.getEntityInheritanceType();
		if ( inheritanceType == InheritanceType.NO_INHERITANCE ) {
			// root, also doubles as a type check since the cast would fail
			return makeEntityBinding( (RootEntityDescriptor) entityDescriptor );
		}
		else {
			if ( entityDescriptor.getSuperEntityName() == null ) {
				throw new MappingException(
						"Encountered inheritance strategy, but no super type found",
						entityDescriptor.getOrigin()
				);
			}

			if ( inheritanceType == InheritanceType.SINGLE_TABLE ) {
				// discriminator subclassing
				return makeEntityBinding( (DiscriminatorSubClassEntityDescriptor) entityDescriptor );
			}
			else if ( inheritanceType == InheritanceType.JOINED ) {
				// joined subclassing
				return makeEntityBinding( (JoinedSubClassEntityDescriptor) entityDescriptor );
			}
			else if ( inheritanceType == InheritanceType.TABLE_PER_CLASS ) {
				return makeEntityBinding( (UnionSubClassEntityDescriptor) entityDescriptor );
			}
			else {
				throw new IllegalStateException( "Unexpected inheritance type [" + inheritanceType + "]" );
			}
		}
	}

	protected EntityBinding makeEntityBinding(RootEntityDescriptor entityDescriptor) {
		final EntityBinding entityBinding = new EntityBinding();

		final Entity entity = new Entity( entityDescriptor.getEntityName(), null, bindingContext.makeJavaType( entityDescriptor.getClassName() ) );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, entityDescriptor );

		entityBinding.setMutable( entityDescriptor.isMutable() );
		entityBinding.setExplicitPolymorphism( entityDescriptor.isExplicitPolymorphism() );
		entityBinding.setWhereFilter( entityDescriptor.getWhereFilter() );
		entityBinding.setRowId( entityDescriptor.getRowId() );
		entityBinding.setOptimisticLockStyle( entityDescriptor.getOptimisticLockStyle() );
		entityBinding.setCaching( entityDescriptor.getCaching() );

		return entityBinding;
	}

	protected EntityBinding makeEntityBinding(DiscriminatorSubClassEntityDescriptor entityDescriptor) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();

		final Entity entity = new Entity( entityDescriptor.getEntityName(), null, bindingContext.makeJavaType( entityDescriptor.getClassName() ) );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, entityDescriptor );

		return entityBinding;
	}

	protected EntityBinding makeEntityBinding(JoinedSubClassEntityDescriptor entityDescriptor) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();

		final Entity entity = new Entity( entityDescriptor.getEntityName(), null, bindingContext.makeJavaType( entityDescriptor.getClassName() ) );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, entityDescriptor );

		return entityBinding;
	}

	protected EntityBinding makeEntityBinding(UnionSubClassEntityDescriptor entityDescriptor) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();

		final Entity entity = new Entity( entityDescriptor.getEntityName(), null, bindingContext.makeJavaType( entityDescriptor.getClassName() ) );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, entityDescriptor );

		return entityBinding;
	}

	protected void performBasicEntityBind(EntityBinding entityBinding, EntityDescriptor entityDescriptor) {
		entityBinding.setInheritanceType( entityDescriptor.getEntityInheritanceType() );

		entityBinding.setJpaEntityName( entityDescriptor.getJpaEntityName() );
		entityBinding.setEntityMode( entityDescriptor.getEntityMode() );

		if ( entityDescriptor.getEntityMode() == EntityMode.POJO ) {
			if ( entityDescriptor.getProxyInterfaceName() != null ) {
				entityBinding.setProxyInterfaceType( bindingContext.makeJavaType( entityDescriptor.getProxyInterfaceName() ) );
				entityBinding.setLazy( true );
			}
			else if ( entityDescriptor.isLazy() ) {
				entityBinding.setProxyInterfaceType( entityBinding.getEntity().getJavaType() );
				entityBinding.setLazy( true );
			}
		}
		else {
			entityBinding.setProxyInterfaceType( new JavaType( Map.class ) );
			entityBinding.setLazy( entityDescriptor.isLazy() );
		}

		entityBinding.setCustomEntityTuplizerClass( entityDescriptor.getCustomEntityTuplizerClass() );
		entityBinding.setCustomEntityPersisterClass( entityDescriptor.getCustomEntityPersisterClass() );

		entityBinding.setMetaAttributeContext( entityDescriptor.getMetaAttributeContext() );

		entityBinding.setDynamicUpdate( entityDescriptor.isDynamicUpdate() );
		entityBinding.setDynamicInsert( entityDescriptor.isDynamicInsert() );
		entityBinding.setBatchSize( entityDescriptor.getBatchSize() );
		entityBinding.setSelectBeforeUpdate( entityDescriptor.isSelectBeforeUpdate() );
		entityBinding.setAbstract( entityDescriptor.isAbstract() );

		entityBinding.setCustomLoaderName( entityDescriptor.getCustomLoaderName() );
		entityBinding.setCustomInsert( entityDescriptor.getCustomInsert() );
		entityBinding.setCustomUpdate( entityDescriptor.getCustomUpdate() );
		entityBinding.setCustomDelete( entityDescriptor.getCustomDelete() );

		if ( entityDescriptor.getSynchronizedTableNames() != null ) {
			entityBinding.addSynchronizedTableNames( entityDescriptor.getSynchronizedTableNames() );
		}
	}
}
