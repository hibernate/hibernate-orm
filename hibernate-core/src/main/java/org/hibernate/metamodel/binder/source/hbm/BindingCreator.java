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

import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.binder.MappingException;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.SubclassEntityElement;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLCacheElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLJoinedSubclassElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlDeleteElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlInsertElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlUpdateElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSubclassElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSynchronizeElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLTuplizerElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLUnionSubclassElement;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * @author Steve Ebersole
 */
public class BindingCreator {
	private final HbmBindingContext bindingContext;
	private final List<String> processedEntityNames;

	public BindingCreator(HbmBindingContext bindingContext, List<String> processedEntityNames) {
		this.bindingContext = bindingContext;
		this.processedEntityNames = processedEntityNames;
	}

	public EntityBinding createEntityBinding(EntityElement entityElement, String containingSuperEntityName) {
		final String entityName = bindingContext.determineEntityName( entityElement );
		if ( processedEntityNames.contains( entityName ) ) {
			return bindingContext.getMetadataImplementor().getEntityBinding( entityName );
		}

		final EntityBinding entityBinding = doEntityBindingCreation( entityElement, containingSuperEntityName );

		bindingContext.getMetadataImplementor().addEntity( entityBinding );
		processedEntityNames.add( entityBinding.getEntity().getName() );
		return entityBinding;
	}

	private EntityBinding doEntityBindingCreation(EntityElement entityElement, String containingSuperEntityName) {
		if ( XMLHibernateMapping.XMLClass.class.isInstance( entityElement ) ) {
			return makeEntityBinding( (XMLHibernateMapping.XMLClass) entityElement );
		}
		else {
			String superEntityName = containingSuperEntityName;
			if ( superEntityName == null ) {
				final SubclassEntityElement subclass = (SubclassEntityElement) entityElement;
				superEntityName = bindingContext.qualifyClassName( subclass.getExtends() );
			}

			if ( superEntityName == null) {
				throw new MappingException(
						"Encountered inheritance strategy, but no super type found",
						bindingContext.getOrigin()
				);
			}

			if ( XMLSubclassElement.class.isInstance( entityElement ) ) {
				return makeEntityBinding( (XMLSubclassElement) entityElement, superEntityName );
			}
			else if ( XMLJoinedSubclassElement.class.isInstance( entityElement ) ) {
				return makeEntityBinding( (XMLJoinedSubclassElement) entityElement, superEntityName );
			}
			else if ( XMLUnionSubclassElement.class.isInstance( entityElement ) ) {
				return makeEntityBinding( (XMLUnionSubclassElement) entityElement, superEntityName );
			}
			else {
				throw new MappingException(
						"unknown type of class or subclass: " + entityElement.getClass().getName(),
						bindingContext.getOrigin()
				);
			}
		}
	}

	protected EntityBinding makeEntityBinding(XMLHibernateMapping.XMLClass xmlClass) {
		final EntityBinding entityBinding = new EntityBinding();
		// todo : this is actually not correct
		// 		the problem is that we need to know whether we have mapped subclasses which happens later
		//		one option would be to simply reset the InheritanceType at that time.
		entityBinding.setInheritanceType( InheritanceType.NO_INHERITANCE );
		entityBinding.setRoot( true );

		final String entityName = bindingContext.determineEntityName( xmlClass );
		final String verbatimClassName = xmlClass.getName();

		final EntityMode entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;
		entityBinding.setEntityMode( entityMode );

		final String className;
		if ( entityMode == EntityMode.POJO ) {
			className = bindingContext.qualifyClassName( verbatimClassName );
		}
		else {
			className = null;
		}

		Entity entity = new Entity( entityName, className, bindingContext.makeClassReference( className ), null );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, xmlClass );

		entityBinding.setMutable( xmlClass.isMutable() );
		entityBinding.setExplicitPolymorphism( "explicit".equals( xmlClass.getPolymorphism() ) );
		entityBinding.setWhereFilter( xmlClass.getWhere() );
		entityBinding.setRowId( xmlClass.getRowid() );
		entityBinding.setOptimisticLockStyle( interpretOptimisticLockStyle( xmlClass ) );
		entityBinding.setCaching( interpretCaching( xmlClass, entityName ) );

		return entityBinding;
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

	private OptimisticLockStyle interpretOptimisticLockStyle(XMLHibernateMapping.XMLClass entityClazz) {
		final String optimisticLockModeString = Helper.getStringValue(
				entityClazz.getOptimisticLock(),
				"version"
		);
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

	protected EntityBinding makeEntityBinding(XMLSubclassElement subclassElement, String superEntityName) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.SINGLE_TABLE );

		final String entityName = bindingContext.determineEntityName( subclassElement );
		final String verbatimClassName = subclassElement.getName();

		final EntityMode entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;
		entityBinding.setEntityMode( entityMode );

		final String className;
		if ( entityMode == EntityMode.POJO ) {
			className = bindingContext.qualifyClassName( verbatimClassName );
		}
		else {
			className = null;
		}

		final Entity entity = new Entity( entityName, className, bindingContext.makeClassReference( className ), null );
		entityBinding.setEntity( entity );


		performBasicEntityBind( entityBinding, subclassElement );

		return entityBinding;
	}

	protected EntityBinding makeEntityBinding(XMLJoinedSubclassElement joinedEntityElement, String superEntityName) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.JOINED );

		final String entityName = bindingContext.determineEntityName( joinedEntityElement );
		final String verbatimClassName = joinedEntityElement.getName();

		final EntityMode entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;
		entityBinding.setEntityMode( entityMode );

		final String className;
		if ( entityMode == EntityMode.POJO ) {
			className = bindingContext.qualifyClassName( verbatimClassName );
		}
		else {
			className = null;
		}

		final Entity entity = new Entity( entityName, className, bindingContext.makeClassReference( className ), null );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, joinedEntityElement );

		return entityBinding;
	}

	protected EntityBinding makeEntityBinding(XMLUnionSubclassElement unionEntityElement, String superEntityName) {
		// temporary!!!

		final EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( InheritanceType.TABLE_PER_CLASS );

		final String entityName = bindingContext.determineEntityName( unionEntityElement );
		final String verbatimClassName = unionEntityElement.getName();

		final EntityMode entityMode = verbatimClassName == null ? EntityMode.MAP : EntityMode.POJO;
		entityBinding.setEntityMode( entityMode );

		final String className;
		if ( entityMode == EntityMode.POJO ) {
			className = bindingContext.qualifyClassName( verbatimClassName );
		}
		else {
			className = null;
		}

		final Entity entity = new Entity( entityName, className, bindingContext.makeClassReference( className ), null );
		entityBinding.setEntity( entity );

		performBasicEntityBind( entityBinding, unionEntityElement );

		return entityBinding;
	}

	@SuppressWarnings( {"unchecked"})
	protected void performBasicEntityBind(EntityBinding entityBinding, EntityElement entityElement) {
		entityBinding.setJpaEntityName( null );

		final String proxy = entityElement.getProxy();
		final boolean isLazy = entityElement.isLazy() == null
				? true
				: entityElement.isLazy();
		if ( entityBinding.getEntityMode() == EntityMode.POJO ) {
			if ( proxy != null ) {
				entityBinding.setProxyInterfaceType(
						bindingContext.makeClassReference(
								bindingContext.qualifyClassName( proxy )
						)
				);
				entityBinding.setLazy( true );
			}
			else if ( isLazy ) {
				entityBinding.setProxyInterfaceType( entityBinding.getEntity().getClassReferenceUnresolved() );
				entityBinding.setLazy( true );
			}
		}
		else {
			entityBinding.setProxyInterfaceType( new Value( Map.class ) );
			entityBinding.setLazy( isLazy );
		}

		final String customTuplizerClassName = extractCustomTuplizerClassName( entityElement, entityBinding.getEntityMode() );
		if ( customTuplizerClassName != null ) {
			entityBinding.setCustomEntityTuplizerClass( bindingContext.<EntityTuplizer>locateClassByName( customTuplizerClassName ) );
		}

		if ( entityElement.getPersister() != null ) {
			entityBinding.setCustomEntityPersisterClass( bindingContext.<EntityPersister>locateClassByName( entityElement.getPersister() ) );
		}

		entityBinding.setMetaAttributeContext(
				Helper.extractMetaAttributeContext(
						entityElement.getMeta(), true, bindingContext.getMetaAttributeContext()
				)
		);

		entityBinding.setDynamicUpdate( entityElement.isDynamicUpdate() );
		entityBinding.setDynamicInsert( entityElement.isDynamicInsert() );
		entityBinding.setBatchSize( Helper.getIntValue( entityElement.getBatchSize(), 0 ) );
		entityBinding.setSelectBeforeUpdate( entityElement.isSelectBeforeUpdate() );
		entityBinding.setAbstract( entityElement.isAbstract() );

		if ( entityElement.getLoader() != null ) {
			entityBinding.setCustomLoaderName( entityElement.getLoader().getQueryRef() );
		}

		final XMLSqlInsertElement sqlInsert = entityElement.getSqlInsert();
		if ( sqlInsert != null ) {
			entityBinding.setCustomInsert( Helper.buildCustomSql( sqlInsert ) );
		}

		final XMLSqlDeleteElement sqlDelete = entityElement.getSqlDelete();
		if ( sqlDelete != null ) {
			entityBinding.setCustomDelete( Helper.buildCustomSql( sqlDelete ) );
		}

		final XMLSqlUpdateElement sqlUpdate = entityElement.getSqlUpdate();
		if ( sqlUpdate != null ) {
			entityBinding.setCustomUpdate( Helper.buildCustomSql( sqlUpdate ) );
		}

		if ( entityElement.getSynchronize() != null ) {
			for ( XMLSynchronizeElement synchronize : entityElement.getSynchronize() ) {
				entityBinding.addSynchronizedTable( synchronize.getTable() );
			}
		}
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
}
