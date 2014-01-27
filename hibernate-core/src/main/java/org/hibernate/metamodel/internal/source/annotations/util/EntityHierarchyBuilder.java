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
package org.hibernate.metamodel.internal.source.annotations.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.AccessType;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.EntityHierarchyImpl;
import org.hibernate.metamodel.internal.source.annotations.JoinedSubclassEntitySourceImpl;
import org.hibernate.metamodel.internal.source.annotations.RootEntitySourceImpl;
import org.hibernate.metamodel.internal.source.annotations.SubclassEntitySourceImpl;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.internal.source.annotations.entity.RootEntityClass;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;

/**
 * Given a (jandex) annotation index build processes all classes with JPA relevant annotations and pre-orders
 * JPA entities respectively their inheritance hierarchy.
 *
 * @author Hardy Ferentschik
 * @author Strong Liu
 */
public class EntityHierarchyBuilder {
	private static final Logger LOG = Logger.getLogger(
			EntityHierarchyBuilder.class);


	/**
	 * Pre-processes the annotated entities from the index and create a set of entity hierarchies which can be bound
	 * to the metamodel.
	 *
	 * @param bindingContext The binding context, giving access to needed services and information
	 *
	 * @return a set of {@code EntityHierarchy} instances.
	 */
	public static Set<EntityHierarchy> createEntityHierarchies(AnnotationBindingContext bindingContext) {
		Set<EntityHierarchy> hierarchies = new HashSet<EntityHierarchy>();
		Map<DotName,DotName> processedEntities = new HashMap<DotName, DotName>(  );
		Map<DotName, List<ClassInfo>> classToDirectSubClassMap = new HashMap<DotName, List<ClassInfo>>();
		IndexView index = bindingContext.getIndex();
		for ( ClassInfo classInfo : index.getKnownClasses() ) {
			if ( processedEntities.containsKey( classInfo.name() ) ) {
				continue;
			}
			if ( !isEntityClass( classInfo ) ) {
				continue;
			}


			ClassInfo rootClassInfo = findRootEntityClassInfo( index, classInfo, bindingContext );
			List<ClassInfo> rootClassWithAllSubclasses = new ArrayList<ClassInfo>();

			// collect the current root entity and all its subclasses
			processHierarchy(
					bindingContext,
					rootClassInfo,
					rootClassWithAllSubclasses,
					processedEntities,
					classToDirectSubClassMap
			);
			boolean hasSubclasses = rootClassWithAllSubclasses.size() > 1;
			// using the root entity, resolve generic parameters in the whole class hierarchy
			final Map<String, ResolvedTypeWithMembers> resolvedTypeWithMembers = resolveGenerics(
					rootClassInfo.toString(),
					bindingContext
			);
			List<ClassInfo> mappedSuperclasses =  findMappedSuperclasses( index, rootClassInfo );

			// the root entity might have some mapped super classes which we have to take into consideration
			// for inheritance type and default access
			rootClassWithAllSubclasses.addAll( mappedSuperclasses );

			AccessType defaultAccessType = determineDefaultAccessType( rootClassWithAllSubclasses );
			InheritanceType hierarchyInheritanceType = determineInheritanceType(
					rootClassInfo,
					rootClassWithAllSubclasses
			);

			// create the root entity source
			RootEntityClass rootEntityClass = new RootEntityClass(
					rootClassInfo,
					resolvedTypeWithMembers,
					mappedSuperclasses,
					defaultAccessType,
					hierarchyInheritanceType,
					hasSubclasses,
					bindingContext
			);
			RootEntitySourceImpl rootSource = new RootEntitySourceImpl( rootEntityClass );

			addSubclassEntitySources(
					bindingContext,
					classToDirectSubClassMap,
					resolvedTypeWithMembers,
					defaultAccessType,
					hierarchyInheritanceType,
					rootEntityClass,
					rootSource
			);

			hierarchies.add( new EntityHierarchyImpl( rootSource, hierarchyInheritanceType ) );
		}
		return hierarchies;
	}

	private static void addSubclassEntitySources(
			AnnotationBindingContext bindingContext,
			Map<DotName, List<ClassInfo>> classToDirectSubClassMap,
			Map<String, ResolvedTypeWithMembers> resolvedTypeWithMembers,
			AccessType defaultAccessType,
			InheritanceType hierarchyInheritanceType,
			EntityClass entityClass,
			EntitySource entitySource) {
		List<ClassInfo> subClassInfoList = classToDirectSubClassMap.get( DotName.createSimple( entitySource.getClassName() ) );
		if ( subClassInfoList == null ) {
			return;
		}
		for ( ClassInfo subClassInfo : subClassInfoList ) {
			resolvedTypeWithMembers.putAll( resolveGenerics( subClassInfo.name().toString(), bindingContext ) );
			 ResolvedTypeWithMembers typeWithMembers = resolvedTypeWithMembers.get( subClassInfo.toString() );
			if ( typeWithMembers == null ) {
				throw new AssertionFailure( "Missing generic information for " + subClassInfo.toString() );
			}
			EntityClass subclassEntityClass = new EntityClass(
					subClassInfo,
					typeWithMembers,
					entityClass,
					defaultAccessType,
					hierarchyInheritanceType,
					bindingContext
			);
			SubclassEntitySource subclassEntitySource = hierarchyInheritanceType == InheritanceType.JOINED ?
					new JoinedSubclassEntitySourceImpl( subclassEntityClass, entitySource )
					: new SubclassEntitySourceImpl( subclassEntityClass, entitySource );
			entitySource.add( subclassEntitySource );
			addSubclassEntitySources(
					bindingContext,
					classToDirectSubClassMap,
					resolvedTypeWithMembers,
					defaultAccessType,
					hierarchyInheritanceType,
					subclassEntityClass,
					subclassEntitySource
			);
		}
	}

	/**
	 * Finds the root entity starting at the entity given by {@code info}. The root entity is not the highest superclass
	 * in a java type sense, but the highest superclass which is also an entity (annotated w/ {@code @Entity}.
	 *
	 * @param index the annotation repository
	 * @param info the class info representing an entity
	 *
	 * @return Finds the root entity starting at the entity given by {@code info}
	 */
	private static ClassInfo findRootEntityClassInfo(IndexView index, ClassInfo info, AnnotationBindingContext bindingContext) {
		ClassInfo rootEntity = info;

		DotName superName = info.superName();
		ClassInfo tmpInfo;
		// walk up the hierarchy until java.lang.Object
		while ( !JandexHelper.OBJECT.equals( superName ) ) {
			tmpInfo = index.getClassByName( superName );
			if ( tmpInfo == null && superName != null ) {
				Class clazz = bindingContext.locateClassByName( superName.toString() );
				if ( clazz != null ) {
					throw new AnnotationException(
							info.name()
									.toString() + "'s parent class [" + clazz.getName() + "] is not added into Jandex repository"
					);
				}else {
					throw new AnnotationException(
							info.name()
									.toString() + "'s parent class [" + superName.toString() + "] doesn't exist in classpath"
					);
				}
			}
			if ( isEntityClass( tmpInfo ) ) {
				rootEntity = tmpInfo;
			}
			superName = tmpInfo.superName();
		}
		return rootEntity;
	}

	private static List<ClassInfo> findMappedSuperclasses(IndexView index, ClassInfo info) {
		List<ClassInfo> mappedSuperclasses = new ArrayList<ClassInfo>(  );
		DotName superName = info.superName();
		// walk up the hierarchy until java.lang.Object
		while ( !JandexHelper.OBJECT.equals( superName ) ) {
			ClassInfo tmpInfo = index.getClassByName( superName );
			if ( isMappedSuperclass( tmpInfo ) ) {
				mappedSuperclasses.add( tmpInfo );
			}
			superName = tmpInfo.superName();
		}
		return  mappedSuperclasses;
	}

	/**
	 * This method does several things.
	 * <ul>
	 * <li>Collect all java subclasses (recursive) of {@code classInfo} in {@code rootClassWithAllSubclasses}. </li>
	 * <li>Keeping track of all processed classed annotated with {@code @Entity}</li>
	 * <li>Building up a map of class to direct subclass list</li>
	 * </ul>
	 *
	 * @param bindingContext the binding context
	 * @param classInfo the current class info
	 * @param rootClassWithAllSubclasses used to collect all classes in the hierarchy starting at {@code classInfo}
	 * @param processedEntities Used to keep track of all processed entities
	 * @param classToDirectSubclassMap Create a map of class to direct subclass
	 */
	private static void processHierarchy(AnnotationBindingContext bindingContext,
										 ClassInfo classInfo,
										 List<ClassInfo> rootClassWithAllSubclasses,
										 Map<DotName,DotName> processedEntities,
										 Map<DotName, List<ClassInfo>> classToDirectSubclassMap) {
		processedEntities.put( classInfo.name(), classInfo.name() );
		rootClassWithAllSubclasses.add( classInfo );
		Collection<ClassInfo> subClasses = bindingContext.getIndex().getKnownDirectSubclasses( classInfo.name() );

		for ( ClassInfo subClassInfo : subClasses ) {
			if ( !isEntityClass( subClassInfo ) ) {
				MappingAssertion.assertSubEntityIsNotEmbeddableNorMappedSuperclass( subClassInfo );
				continue;
			}
			addSubClassToSubclassMap( classInfo.name(), subClassInfo, classToDirectSubclassMap );
			processHierarchy(
					bindingContext,
					subClassInfo,
					rootClassWithAllSubclasses,
					processedEntities,
					classToDirectSubclassMap
			);
		}
	}

	private static void addSubClassToSubclassMap(DotName name, ClassInfo subClassInfo, Map<DotName, List<ClassInfo>> classToDirectSubclassMap) {
		if ( classToDirectSubclassMap.containsKey( name ) ) {
			classToDirectSubclassMap.get( name ).add( subClassInfo );
		}
		else {
			List<ClassInfo> subclassList = new ArrayList<ClassInfo>();
			subclassList.add( subClassInfo );
			classToDirectSubclassMap.put( name, subclassList );
		}
	}

	/**
	 * Checks whether the class info represents an entity.
	 *
	 * @param info the jandex class info
	 *
	 * @return {@code true} if the class represented by {@code info} is annotated with {@code @Entity}, {@code false} otherwise.
	 */
	private static boolean isEntityClass(ClassInfo info) {
		if ( info == null ) {
			return false;
		}

		// we are only interested in building the class hierarchies for @Entity
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation( info, JPADotNames.ENTITY );
		if ( jpaEntityAnnotation == null ) {
			return false;
		}

		// some sanity checks
		AnnotationInstance mappedSuperClassAnnotation = JandexHelper.getSingleAnnotation(
				info, JPADotNames.MAPPED_SUPERCLASS
		);
		String className = info.toString();
		MappingAssertion.assertNotEntityAndMappedSuperClass(
				jpaEntityAnnotation,
				mappedSuperClassAnnotation,
				className
		);

		AnnotationInstance embeddableAnnotation = JandexHelper.getSingleAnnotation(
				info, JPADotNames.EMBEDDABLE
		);
		MappingAssertion.assertNotEntityAndEmbeddable(
				jpaEntityAnnotation,
				embeddableAnnotation,
				className );

		return true;
	}

	/**
	 * Checks whether the class info represents a mapped superclass.
	 *
	 * @param info the jandex class info
	 *
	 * @return {@code true} if the class represented by {@code info} is annotated with {@code @MappedSuperclass}, {@code false} otherwise.
	 */
	private static boolean isMappedSuperclass(ClassInfo info) {
		if ( info == null ) {
			return false;
		}

		// we are only interested in building the class hierarchies for @Entity
		AnnotationInstance mappedSuperclassAnnotation = JandexHelper.getSingleAnnotation(
				info,
				JPADotNames.MAPPED_SUPERCLASS
		);
		return mappedSuperclassAnnotation != null;
	}

	/**
	 * @param classes the classes in the hierarchy
	 *
	 * @return Returns the default access type for the configured class hierarchy independent of explicit
	 *         {@code AccessType} annotations. The default access type is determined by the placement of the
	 *         annotations.
	 */
	private static AccessType determineDefaultAccessType(List<ClassInfo> classes) {
		AccessType accessTypeByEmbeddedIdPlacement = null;
		AccessType accessTypeByIdPlacement = null;
		for ( ClassInfo info : classes ) {
			List<AnnotationInstance> idAnnotations = info.annotations().get( JPADotNames.ID );
			List<AnnotationInstance> embeddedIdAnnotations = info.annotations().get( JPADotNames.EMBEDDED_ID );

			if ( CollectionHelper.isNotEmpty( embeddedIdAnnotations ) ) {
				accessTypeByEmbeddedIdPlacement = determineAccessTypeByIdPlacement( embeddedIdAnnotations );
			}
			if ( CollectionHelper.isNotEmpty( idAnnotations ) ) {
				accessTypeByIdPlacement = determineAccessTypeByIdPlacement( idAnnotations );
			}
		}
		if ( accessTypeByEmbeddedIdPlacement != null ) {
			return accessTypeByEmbeddedIdPlacement;
		}
		else if ( accessTypeByIdPlacement != null ) {
			return accessTypeByIdPlacement;
		}
		else {
			return throwIdNotFoundAnnotationException( classes );
		}
	}

	private static AccessType determineAccessTypeByIdPlacement(List<AnnotationInstance> idAnnotations) {
		AccessType accessType = null;
		for ( AnnotationInstance annotation : idAnnotations ) {
			AccessType tmpAccessType;
			if ( annotation.target() instanceof FieldInfo ) {
				tmpAccessType = AccessType.FIELD;
			}
			else if ( annotation.target() instanceof MethodInfo ) {
				tmpAccessType = AccessType.PROPERTY;
			}
			else {
				throw new AnnotationException( "Invalid placement of @Id annotation" );
			}

			if ( accessType == null ) {
				accessType = tmpAccessType;
			}
			else {
				if ( !accessType.equals( tmpAccessType ) ) {
					throw new AnnotationException( "Inconsistent placement of @Id annotation within hierarchy " );
				}
			}
		}
		return accessType;
	}

	private static InheritanceType determineInheritanceType(ClassInfo rootClassInfo, List<ClassInfo> classes) {
		if(classes.size() == 1) {
			return InheritanceType.NO_INHERITANCE;
		}

		// if we have more than one entity class the default is SINGLE_TABLE
		InheritanceType inheritanceType = InheritanceType.SINGLE_TABLE;
		AnnotationInstance inheritanceAnnotation = JandexHelper.getSingleAnnotation(
				rootClassInfo, JPADotNames.INHERITANCE
		);
		if ( inheritanceAnnotation != null && inheritanceAnnotation.value( "strategy" )!=null) {
			String enumName = inheritanceAnnotation.value( "strategy" ).asEnum();
			javax.persistence.InheritanceType jpaInheritanceType = Enum.valueOf(
					javax.persistence.InheritanceType.class, enumName
			);
			inheritanceType = InheritanceType.get( jpaInheritanceType );
		}

		// sanity check that the is no other @Inheritance annotation in the hierarchy
		for ( ClassInfo info : classes ) {
			if ( rootClassInfo.equals( info ) ) {
				continue;
			}
			inheritanceAnnotation = JandexHelper.getSingleAnnotation(
					info, JPADotNames.INHERITANCE
			);
			if ( inheritanceAnnotation != null ) {
				LOG.warn(
						String.format(
								"The inheritance type for %s should be specified only on the root entity %s.  Ignoring...",
								info.name(),
								rootClassInfo.name().toString()
						)
				);
			}
		}

		return inheritanceType;
	}

	private static AccessType throwIdNotFoundAnnotationException(List<ClassInfo> classes) {
		StringBuilder builder = new StringBuilder();
		builder.append( "Unable to determine identifier attribute for class hierarchy consisting of the classe(s) " );
		builder.append( hierarchyListString( classes ) );
		throw new AnnotationException( builder.toString() );
	}

	private static String hierarchyListString(List<ClassInfo> classes) {
		StringBuilder builder = new StringBuilder();
		builder.append( "[" );

		int count = 0;
		for ( ClassInfo info : classes ) {
			builder.append( info.name().toString() );
			if ( count < classes.size() - 1 ) {
				builder.append( ", " );
			}
			count++;
		}
		builder.append( "]" );
		return builder.toString();
	}

	/**
	 * TODO this is incorrect, we should do it from bottom to top, not inverse, since the actual type info should be dedined in the sub class.
	 * we need this info to resolve super class type.
	 */
	private static Map<String, ResolvedTypeWithMembers> resolveGenerics(String className, AnnotationBindingContext bindingContext) {
		final Map<String, ResolvedTypeWithMembers> resolvedTypes = new HashMap<String, ResolvedTypeWithMembers>();
		final Class<?> clazz = bindingContext.locateClassByName( className );
		ResolvedType resolvedType = bindingContext.getTypeResolver().resolve( clazz );
		while ( resolvedType!= null && !Object.class.equals( resolvedType.getErasedType() ) ) {
			final ResolvedTypeWithMembers resolvedTypeWithMembers = bindingContext.getMemberResolver()
					.resolve( resolvedType, null, null );
			resolvedTypes.put( className, resolvedTypeWithMembers );

			resolvedType = resolvedType.getParentClass();
			if ( resolvedType != null ) {
				className = resolvedType.getErasedType().getName();
			}
		}

		return resolvedTypes;
	}
}


