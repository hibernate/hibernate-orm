/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSubclassEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbInheritanceImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import jakarta.persistence.InheritanceType;

/**
 * @author Steve Ebersole
 */
public class TransformationPreprocessor {
	static void preprocessHbmXml(
			List<Binding<JaxbHbmHibernateMapping>> hbmXmlBindings,
			TransformationState transformationState) {
		final EntityMappingConsumer entityMappingConsumer = new EntityMappingConsumer( transformationState );
		final Map<String, JaxbEntityImpl> rootClassesMap = new HashMap<>();

		processStructuredHierarchies( hbmXmlBindings, rootClassesMap, entityMappingConsumer );
		processSeparatedHierarchies( hbmXmlBindings, rootClassesMap, entityMappingConsumer );
	}

	private static void processStructuredHierarchies(
			Collection<Binding<JaxbHbmHibernateMapping>> hbmXmlBindings,
			Map<String, JaxbEntityImpl> rootClassesMap,
			EntityMappingConsumer entityMappingConsumer) {
		hbmXmlBindings.forEach( (hbmBinding) -> {
			processStructuredHierarchies( rootClassesMap, entityMappingConsumer, hbmBinding );
		} );
	}

	private static void processStructuredHierarchies(
			Map<String, JaxbEntityImpl> rootClassesMap,
			EntityMappingConsumer entityMappingConsumer,
			Binding<JaxbHbmHibernateMapping> hbmBinding) {
		final JaxbHbmHibernateMapping hibernateMapping = hbmBinding.getRoot();
		hibernateMapping.getClazz().forEach( (hbmRoot) -> {
			final JaxbEntityImpl rootJaxbEntity = makeJaxbEntity( hbmRoot, hibernateMapping );
			final String entityName = determineEntityName( hbmRoot, hibernateMapping );

			entityMappingConsumer.accept( hbmBinding, entityName, hbmRoot, rootJaxbEntity );
			rootClassesMap.put( entityName, rootJaxbEntity );

			if ( CollectionHelper.isNotEmpty( hbmRoot.getSubclass() ) ) {
				applyInheritanceStrategy( entityName, rootJaxbEntity, InheritanceType.SINGLE_TABLE );
				processStructuredDiscriminatorSubclasses(
						hbmRoot.getSubclass(),
						entityName,
						hbmBinding,
						entityMappingConsumer
				);
			}

			if ( CollectionHelper.isNotEmpty( hbmRoot.getJoinedSubclass() ) ) {
				applyInheritanceStrategy( entityName, rootJaxbEntity, InheritanceType.JOINED );
				processStructuredJoinedSubclasses(
						hbmRoot.getJoinedSubclass(),
						entityName,
						hbmBinding,
						entityMappingConsumer
				);
			}

			if ( CollectionHelper.isNotEmpty( hbmRoot.getUnionSubclass() ) ) {
				applyInheritanceStrategy( entityName, rootJaxbEntity, InheritanceType.TABLE_PER_CLASS );
				processStructuredUnionSubclasses(
						hbmRoot.getUnionSubclass(),
						entityName,
						hbmBinding,
						entityMappingConsumer
				);
			}
		} );
	}

	private static void applyInheritanceStrategy(String entityName, JaxbEntityImpl rootJaxbEntity, InheritanceType strategy) {
		final JaxbInheritanceImpl existing = rootJaxbEntity.getInheritance();
		if ( existing != null ) {
			if ( existing.getStrategy() != null && existing.getStrategy() != strategy ) {
				throw new IllegalStateException( String.format(
						Locale.ROOT,
						"Root entity `%s` defined a mix of inheritance strategies; at least %s and %s",
						entityName,
						strategy,
						existing.getStrategy()
				) );
			}
			existing.setStrategy( strategy );
		}
		else {
			final JaxbInheritanceImpl created = new JaxbInheritanceImpl();
			created.setStrategy( strategy );
			rootJaxbEntity.setInheritance( created );
		}
	}

	private static void processStructuredDiscriminatorSubclasses(
			List<JaxbHbmDiscriminatorSubclassEntityType> hbmSubclasses,
			String superEntityName,
			Binding<JaxbHbmHibernateMapping> hbmBinding,
			EntityMappingConsumer entityMappingConsumer) {
		hbmSubclasses.forEach( (hbmSubclass) -> {
			final JaxbEntityImpl subclassJaxbEntity = makeJaxbEntity( hbmSubclass, hbmBinding.getRoot() );
			final String subclassEntityName = determineEntityName( hbmSubclass, hbmBinding.getRoot() );
			entityMappingConsumer.accept( hbmBinding, subclassEntityName, hbmSubclass, subclassJaxbEntity );

			subclassJaxbEntity.setExtends( superEntityName );

			if ( CollectionHelper.isNotEmpty( hbmSubclass.getSubclass() ) ) {
				processStructuredDiscriminatorSubclasses(
						hbmSubclass.getSubclass(),
						subclassEntityName,
						hbmBinding,
						entityMappingConsumer
				);
			}
		} );
	}

	private static void processStructuredJoinedSubclasses(
			List<JaxbHbmJoinedSubclassEntityType> hbmSubclasses,
			String superEntityName,
			Binding<JaxbHbmHibernateMapping> hbmBinding,
			EntityMappingConsumer entityMappingConsumer) {
		hbmSubclasses.forEach( (hbmSubclass) -> {
			final JaxbEntityImpl subclassJaxbEntity = makeJaxbEntity( hbmSubclass, hbmBinding.getRoot() );
			final String subclassEntityName = determineEntityName( hbmSubclass, hbmBinding.getRoot() );
			entityMappingConsumer.accept( hbmBinding, subclassEntityName, hbmSubclass, subclassJaxbEntity );

			subclassJaxbEntity.setExtends( superEntityName );

			if ( CollectionHelper.isNotEmpty( hbmSubclass.getJoinedSubclass() ) ) {
				processStructuredJoinedSubclasses(
						hbmSubclass.getJoinedSubclass(),
						subclassEntityName,
						hbmBinding,
						entityMappingConsumer
				);
			}
		} );
	}

	private static void processStructuredUnionSubclasses(
			List<JaxbHbmUnionSubclassEntityType> hbmSubclasses,
			String superEntityName,
			Binding<JaxbHbmHibernateMapping> hbmBinding,
			EntityMappingConsumer entityMappingConsumer) {
		hbmSubclasses.forEach( (hbmSubclass) -> {
			final JaxbEntityImpl subclassJaxbEntity = makeJaxbEntity( hbmSubclass, hbmBinding.getRoot() );
			final String subclassEntityName = determineEntityName( hbmSubclass, hbmBinding.getRoot() );
			entityMappingConsumer.accept( hbmBinding, subclassEntityName, hbmSubclass, subclassJaxbEntity );

			subclassJaxbEntity.setExtends( superEntityName );

			if ( CollectionHelper.isNotEmpty( hbmSubclass.getUnionSubclass() ) ) {
				processStructuredUnionSubclasses(
						hbmSubclass.getUnionSubclass(),
						subclassEntityName,
						hbmBinding,
						entityMappingConsumer
				);
			}
		} );
	}

	private static void processSeparatedHierarchies(
			Collection<Binding<JaxbHbmHibernateMapping>> hbmXmlBindings,
			Map<String, JaxbEntityImpl> rootClassesMap,
			EntityMappingConsumer entityMappingConsumer) {
		hbmXmlBindings.forEach( (hbmBinding) -> {
			processSeparatedHierarchies( rootClassesMap, entityMappingConsumer, hbmBinding );

		} );
	}

	private static void processSeparatedHierarchies(
			Map<String, JaxbEntityImpl> rootClassesMap,
			EntityMappingConsumer entityMappingConsumer,
			Binding<JaxbHbmHibernateMapping> hbmBinding) {
		final JaxbHbmHibernateMapping hibernateMapping = hbmBinding.getRoot();

		processTopLevelDiscriminatedSubclasses( rootClassesMap, entityMappingConsumer, hbmBinding, hibernateMapping );
		processTopLevelJoinedSubclasses( rootClassesMap, entityMappingConsumer, hbmBinding, hibernateMapping );
		processTopLevelUnionSubclasses( rootClassesMap, entityMappingConsumer, hbmBinding, hibernateMapping );
	}

	private static void processTopLevelDiscriminatedSubclasses(
			Map<String, JaxbEntityImpl> rootClassesMap,
			EntityMappingConsumer entityMappingConsumer,
			Binding<JaxbHbmHibernateMapping> hbmBinding,
			JaxbHbmHibernateMapping hibernateMapping) {
		hibernateMapping.getSubclass().forEach( (hbmSubclass) -> {
			final String entityName = determineEntityName( hbmSubclass, hibernateMapping );
			applyExtendedInheritanceStrategy(
					entityName,
					InheritanceType.SINGLE_TABLE,
					hbmSubclass,
					hibernateMapping,
					rootClassesMap,
					hbmBinding.getOrigin()
			);

			final JaxbEntityImpl jaxbEntity = makeJaxbEntity( hbmSubclass, hibernateMapping );
			entityMappingConsumer.accept( hbmBinding, entityName, hbmSubclass, jaxbEntity );

			if ( CollectionHelper.isNotEmpty( hbmSubclass.getSubclass() ) ) {
				processStructuredDiscriminatorSubclasses(
						hbmSubclass.getSubclass(),
						entityName,
						hbmBinding,
						entityMappingConsumer
				);
			}
		} );
	}

	private static void processTopLevelJoinedSubclasses(
			Map<String, JaxbEntityImpl> rootClassesMap,
			EntityMappingConsumer entityMappingConsumer,
			Binding<JaxbHbmHibernateMapping> hbmBinding,
			JaxbHbmHibernateMapping hibernateMapping) {
		hibernateMapping.getJoinedSubclass().forEach( (hbmSubclass) -> {
			final String entityName = determineEntityName( hbmSubclass, hibernateMapping );
			applyExtendedInheritanceStrategy(
					entityName,
					InheritanceType.JOINED,
					hbmSubclass,
					hibernateMapping,
					rootClassesMap,
					hbmBinding.getOrigin()
			);

			final JaxbEntityImpl jaxbEntity = makeJaxbEntity( hbmSubclass, hibernateMapping );
			entityMappingConsumer.accept( hbmBinding, entityName, hbmSubclass, jaxbEntity );

			if ( CollectionHelper.isNotEmpty( hbmSubclass.getJoinedSubclass() ) ) {
				processStructuredJoinedSubclasses(
						hbmSubclass.getJoinedSubclass(),
						entityName,
						hbmBinding,
						entityMappingConsumer
				);
			}
		} );
	}

	private static void processTopLevelUnionSubclasses(
			Map<String, JaxbEntityImpl> rootClassesMap,
			EntityMappingConsumer entityMappingConsumer,
			Binding<JaxbHbmHibernateMapping> hbmBinding,
			JaxbHbmHibernateMapping hibernateMapping) {
		hibernateMapping.getUnionSubclass().forEach( (hbmSubclass) -> {
			final String entityName = determineEntityName( hbmSubclass, hibernateMapping );
			applyExtendedInheritanceStrategy(
					entityName,
					InheritanceType.TABLE_PER_CLASS,
					hbmSubclass,
					hibernateMapping,
					rootClassesMap,
					hbmBinding.getOrigin()
			);

			final JaxbEntityImpl jaxbEntity = makeJaxbEntity( hbmSubclass, hibernateMapping );
			entityMappingConsumer.accept( hbmBinding, entityName, hbmSubclass, jaxbEntity );

			if ( CollectionHelper.isNotEmpty( hbmSubclass.getUnionSubclass() ) ) {
				processStructuredUnionSubclasses(
						hbmSubclass.getUnionSubclass(),
						entityName,
						hbmBinding,
						entityMappingConsumer
				);
			}
		} );
	}

	private static void applyExtendedInheritanceStrategy(
			String entityName,
			InheritanceType strategy,
			JaxbHbmSubclassEntityBaseDefinition hbmSubclass,
			JaxbHbmHibernateMapping hibernateMapping,
			Map<String, JaxbEntityImpl> rootClassesMap,
			Origin origin) {
		if ( StringHelper.isEmpty( hbmSubclass.getExtends() ) ) {
			throw new MappingException( "Separated inheritance mapping did not specify extends", origin );
		}

		// we only have something to do here if the extends names a root entity
		final JaxbEntityImpl superRoot = TransformationState.resolveEntityReference(
				hbmSubclass.getExtends(),
				hibernateMapping,
				rootClassesMap
		);

		if ( superRoot != null ) {
			applyInheritanceStrategy( entityName, superRoot, strategy );
		}
	}

	private static JaxbEntityImpl makeJaxbEntity(
			JaxbHbmEntityBaseDefinition hbmEntity,
			JaxbHbmHibernateMapping hibernateMapping) {
		final JaxbEntityImpl jaxbEntity = new JaxbEntityImpl();
		if ( StringHelper.isNotEmpty( hbmEntity.getName() ) ) {
			jaxbEntity.setClazz( StringHelper.qualifyConditionallyIfNot( hibernateMapping.getPackage(), hbmEntity.getName() ) );
		}
		if ( StringHelper.isNotEmpty( hbmEntity.getEntityName() ) ) {
			jaxbEntity.setName( hbmEntity.getEntityName() );
		}
		return jaxbEntity;
	}

	private static String determineEntityName(EntityInfo entityInfo, JaxbHbmHibernateMapping hibernateMapping) {
		if ( entityInfo.getEntityName() != null ) {
			return entityInfo.getEntityName();
		}
		return StringHelper.qualifyConditionallyIfNot( hibernateMapping.getPackage(), entityInfo.getName() );
	}


	private record EntityMappingConsumer(TransformationState transformationState) {

		public void accept(
					Binding<JaxbHbmHibernateMapping> hbmBinding,
					String registrationName,
					JaxbHbmEntityBaseDefinition hbmEntity,
					JaxbEntityImpl mappingEntity) {
				final Binding<JaxbEntityMappingsImpl> mappingBinding = resolveMappingBinding( hbmBinding );
				mappingBinding.getRoot().getEntities().add( mappingEntity );
				transformationState.getEntityXref().put( hbmEntity, mappingEntity );
				transformationState.getEntityMap().put( registrationName, mappingEntity );
			}

			private Binding<JaxbEntityMappingsImpl> resolveMappingBinding(Binding<JaxbHbmHibernateMapping> hbmBinding) {
				for ( int i = 0; i < transformationState.getHbmBindings().size(); i++ ) {
					if ( hbmBinding == transformationState.getHbmBindings().get( i ) ) {
						return transformationState.getMappingBindings().get( i );
					}
				}

				throw new IllegalStateException( "Could not resolve corresponding mapping binding : " + hbmBinding );
			}
		}
}
