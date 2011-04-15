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
package org.hibernate.metamodel.source.annotations;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.annotations.util.ReflectionHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Represents an entity, mapped superclass or component configured via annotations/xml.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClass {
	private final ConfiguredClass parent;
	private final ClassInfo classInfo;
	private final Class<?> clazz;
	private final boolean isRoot;
	private final AccessType classAccessType;
	private final AccessType hierarchyAccessType;
	private final boolean isMappedSuperClass;
	private final List<MappedProperty> mappedProperties;

	public ConfiguredClass(ClassInfo info, ConfiguredClass parent, AccessType hierarchyAccessType, ServiceRegistry serviceRegistry) {
		this.classInfo = info;
		this.parent = parent;
		this.isRoot = parent == null;
		this.hierarchyAccessType = hierarchyAccessType;

		AnnotationInstance mappedSuperClassAnnotation = assertNotEntityAndMAppedSuperClass();

		this.clazz = serviceRegistry.getService( ClassLoaderService.class ).classForName( info.toString() );
		isMappedSuperClass = mappedSuperClassAnnotation != null;
		classAccessType = determineClassAccessType();
		mappedProperties = collectMappedProperties();
		// make sure the properties are ordered by property name
		Collections.sort( mappedProperties );
	}

	public String getName() {
		return clazz.getName();
	}

	public ClassInfo getClassInfo() {
		return classInfo;
	}

	public ConfiguredClass getParent() {
		return parent;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public boolean isMappedSuperClass() {
		return isMappedSuperClass;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClass" );
		sb.append( "{classInfo=" ).append( classInfo );
		sb.append( '}' );
		return sb.toString();
	}

	private AccessType determineClassAccessType() {
		// default to the hierarchy access type to start with
		AccessType accessType = hierarchyAccessType;

		AnnotationInstance accessAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ACCESS );
		if ( accessAnnotation != null ) {
			accessType = Enum.valueOf( AccessType.class, accessAnnotation.value( "value" ).asEnum() );
		}

		return accessType;
	}

	private List<MappedProperty> collectMappedProperties() {
		Set<String> transientFieldNames = new HashSet<String>();
		Set<String> transientMethodNames = new HashSet<String>();
		populateTransientFieldAndMethodLists( transientFieldNames, transientMethodNames );

		List<Member> classMembers = new ArrayList<Member>();
		Set<String> explicitlyConfiguredAccessMemberNames;
		if ( AccessType.FIELD.equals( classAccessType ) ) {
			explicitlyConfiguredAccessMemberNames = addExplicitAccessMembers( classMembers, MethodInfo.class );
			Field fields[] = clazz.getDeclaredFields();
			Field.setAccessible( fields, true );
			for ( Field field : fields ) {
				if ( !transientFieldNames.contains( field.getName() )
						&& !explicitlyConfiguredAccessMemberNames.contains( field.getName() ) ) {
					classMembers.add( field );
				}
			}
		}
		else {
			explicitlyConfiguredAccessMemberNames = addExplicitAccessMembers( classMembers, FieldInfo.class );
			Method[] methods = clazz.getDeclaredMethods();
			Method.setAccessible( methods, true );
			for ( Method method : methods ) {
				if ( !transientMethodNames.contains( method.getName() )
						&& !explicitlyConfiguredAccessMemberNames.contains( ReflectionHelper.getPropertyName( method ) ) ) {
					classMembers.add( method );
				}
			}
		}

		List<MappedProperty> properties = new ArrayList<MappedProperty>();
		return properties;
	}

	private Set<String> addExplicitAccessMembers(List<Member> classMembers, Class<? extends AnnotationTarget> targetClass) {
		Set<String> explicitAccessMembers = new HashSet<String>();

		List<AnnotationInstance> accessAnnotations = classInfo.annotations().get( JPADotNames.ACCESS );
		if ( accessAnnotations == null ) {
			return explicitAccessMembers;
		}

		for ( AnnotationInstance accessAnnotation : accessAnnotations ) {
			// at this stage we are only interested at annotations defined on fields and methods
			AnnotationTarget target = accessAnnotation.target();
			if ( !target.getClass().equals( targetClass ) ) {
				continue;
			}

			AccessType accessType = Enum.valueOf( AccessType.class, accessAnnotation.value().asEnum() );

			if ( target instanceof MethodInfo && MethodInfo.class.equals( targetClass ) ) {
				// annotating a field with @AccessType(PROPERTY) has not effect
				if ( !AccessType.PROPERTY.equals( accessType ) ) {
					continue;
				}
				Method m;
				try {
					m = clazz.getMethod( ( (MethodInfo) target ).name() );
				}
				catch ( NoSuchMethodException e ) {
					throw new HibernateException(
							"Unable to load method "
									+ ( (MethodInfo) target ).name()
									+ " of class " + clazz.getName()
					);
				}
				classMembers.add( m );
				explicitAccessMembers.add( ReflectionHelper.getPropertyName( m ) );
				continue;
			}

			if ( target instanceof FieldInfo && FieldInfo.class.equals( targetClass ) ) {
				// annotating a method w/ @AccessType(FIELD) has no effect
				if ( !AccessType.FIELD.equals( accessType ) ) {
					continue;
				}
				Field f;
				try {
					f = clazz.getField( ( (FieldInfo) target ).name() );
				}
				catch ( NoSuchFieldException e ) {
					throw new HibernateException(
							"Unable to load field "
									+ ( (FieldInfo) target ).name()
									+ " of class " + clazz.getName()
					);
				}
				classMembers.add( f );
				explicitAccessMembers.add( f.getName() );
				continue;
			}
		}
		return explicitAccessMembers;
	}

	/**
	 * Populates the sets of transient field and method names.
	 *
	 * @param transientFieldNames Set to populate with the field names explicitly marked as @Transient
	 * @param transientMethodNames set to populate with the method names explicitly marked as @Transient
	 */
	private void populateTransientFieldAndMethodLists(Set<String> transientFieldNames, Set<String> transientMethodNames) {
		List<AnnotationInstance> transientMembers = classInfo.annotations().get( JPADotNames.TRANSIENT );
		if ( transientMembers == null ) {
			return;
		}

		for ( AnnotationInstance transientMember : transientMembers ) {
			AnnotationTarget target = transientMember.target();
			if ( target instanceof FieldInfo ) {
				transientFieldNames.add( ( (FieldInfo) target ).name() );
			}
			else {
				transientMethodNames.add( ( (MethodInfo) target ).name() );
			}
		}
	}

	private AnnotationInstance assertNotEntityAndMAppedSuperClass() {
		//@Entity and @MappedSuperclass on the same class leads to a NPE down the road
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ENTITY );
		AnnotationInstance mappedSuperClassAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, JPADotNames.MAPPED_SUPER_CLASS
		);

		if ( jpaEntityAnnotation != null && mappedSuperClassAnnotation != null ) {
			throw new AnnotationException(
					"An entity cannot be annotated with both @Entity and @MappedSuperclass: "
							+ classInfo.name().toString()
			);
		}
		return mappedSuperClassAnnotation;
	}
}


