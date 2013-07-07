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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.AccessType;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.ClassmateHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;

import static org.hibernate.metamodel.spi.binding.SingularAttributeBinding.NaturalIdMutability;

/**
 * Contains information about the access and inheritance type for all classes within a class hierarchy.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddableHierarchy implements Iterable<EmbeddableClass> {
	private final AccessType defaultAccessType;
	private final List<EmbeddableClass> embeddables;
	/**
	 * Builds the configured class hierarchy for a an embeddable class.
	 *
	 * @param embeddableClass the top level embedded class
	 * @param propertyName the name of the property in the entity class embedding this embeddable
	 * @param accessType the access type inherited from the class in which the embeddable gets embedded
	 * @param context the annotation binding context with access to the service registry and the annotation index
	 *
	 * @return a set of {@code ConfiguredClassHierarchy}s. One for each "leaf" entity.
	 */
	public static EmbeddableHierarchy createEmbeddableHierarchy(
			final Class<?> embeddableClass,
			final String propertyName,
			final ResolvedType resolvedType,
			final AccessType accessType,
			final NaturalIdMutability naturalIdMutability,
			final String customTuplizerClass,
			final AnnotationBindingContext context) {

		final ClassInfo embeddableClassInfo = context.getClassInfo( embeddableClass.getName() );
		checkIndexed( embeddableClass, embeddableClassInfo );
		checkEmbeddableAnnotation( embeddableClass, embeddableClassInfo );
		Map<String, ResolvedTypeWithMembers> resolvedTypeWithMembers = ClassmateHelper.resolveClassHierarchyTypesFromAttributeType(
				embeddableClass,
				resolvedType,
				context
		);
		List<ClassInfo> classInfoList = new ArrayList<ClassInfo>();
		Class<?> clazz = embeddableClass;
		while ( clazz != null && !clazz.equals( Object.class ) ) {
			ClassInfo tmpClassInfo = context.getIndex().getClassByName( DotName.createSimple( clazz.getName() ) );
			if ( tmpClassInfo == null ) {
				continue;
			}
			clazz = clazz.getSuperclass();
			classInfoList.add( 0, tmpClassInfo );
		}

		return new EmbeddableHierarchy(
				classInfoList,
				resolvedTypeWithMembers,
				propertyName,
				naturalIdMutability,
				customTuplizerClass,
				context,
				accessType
		);
	}

	private static void checkEmbeddableAnnotation(Class<?> embeddableClass, ClassInfo embeddableClassInfo) {
		if ( JandexHelper.getSingleAnnotation( embeddableClassInfo, JPADotNames.EMBEDDABLE ) == null ) {
			throw new AssertionFailure(
					String.format(
							"The specified class %s is not annotated with @Embeddable even though it is as embeddable",
							embeddableClass.getName()
					)
			);
		}
	}

	private static void checkIndexed(Class<?> embeddableClass, ClassInfo embeddableClassInfo) {
		if ( embeddableClassInfo == null ) {
			throw new AssertionFailure(
					String.format(
							"The specified class %s cannot be found in the annotation index",
							embeddableClass.getName()
					)
			);
		}
	}

	@SuppressWarnings("unchecked")
	private EmbeddableHierarchy(
			final List<ClassInfo> classInfoList,
			final Map<String, ResolvedTypeWithMembers> resolvedTypeWithMembers,
			final String propertyName,
			final NaturalIdMutability naturalIdMutability,
			final String customTuplizerClass,
			final AnnotationBindingContext context,
			final AccessType defaultAccessType) {
		this.defaultAccessType = defaultAccessType;

		this.embeddables = new ArrayList<EmbeddableClass>();
		ConfiguredClass parent = null;
		for ( ClassInfo info : classInfoList ) {
			ResolvedTypeWithMembers fullyResolvedType = resolvedTypeWithMembers.get( info.toString() );
			if ( fullyResolvedType == null ) {
				throw new AssertionFailure( "Unable to find resolved type information for " + info.toString() );
			}
			EmbeddableClass embeddable = new EmbeddableClass(
					info,
					fullyResolvedType,
					propertyName,
					parent,
					defaultAccessType,
					naturalIdMutability,
					customTuplizerClass,
					context
			);
			embeddables.add( embeddable );
			parent = embeddable;
		}
	}


	public AccessType getDefaultAccessType() {
		return defaultAccessType;
	}

	/**
	 * @return An iterator iterating in top down manner over the configured classes in this hierarchy.
	 */
	@Override
	public Iterator<EmbeddableClass> iterator() {
		return embeddables.iterator();
	}

	/**
	 * @return Returns the leaf configured class
	 */
	public EmbeddableClass getLeaf() {
		return embeddables.get( embeddables.size() - 1 );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EmbeddableHierarchy" );
		sb.append( "{defaultAccessType=" ).append( defaultAccessType );
		sb.append( ", embeddables=" ).append( embeddables );
		sb.append( '}' );
		return sb.toString();
	}


}
