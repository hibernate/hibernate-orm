/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.persistence.AccessType;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

/**
 * Implementation of the PersistentAttributeMemberResolver contract that sticks
 * pretty close to the specifics set forth in the JPA specification.  Specifically
 * AccessType does influence where we look for annotations.
 *
 * @author Steve Ebersole
 */
public class StandardPersistentAttributeMemberResolver extends AbstractPersistentAttributeMemberResolver {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StandardPersistentAttributeMemberResolver.class );

	/**
	 * Singleton access
	 */
	public static final StandardPersistentAttributeMemberResolver INSTANCE = new StandardPersistentAttributeMemberResolver();

	@Override
	protected List<MemberDescriptor> resolveAttributesMembers(
			Set<String> transientFieldNames,
			Set<String> transientMethodNames,
			JavaTypeDescriptor managedTypeDescriptor,
			AccessType classLevelAccessType,
			LocalBindingContext localBindingContext) {
		final LinkedHashMap<String,MemberDescriptor> results = new LinkedHashMap<String, MemberDescriptor>();

		collectMembersWithExplicitAccessAnnotation(
				results,
				transientFieldNames,
				transientMethodNames,
				managedTypeDescriptor,
				classLevelAccessType,
				localBindingContext
		);

		collectMembersUsingClassDefinedAccess(
				results,
				transientFieldNames,
				transientMethodNames,
				managedTypeDescriptor,
				classLevelAccessType,
				localBindingContext
		);

		return new ArrayList<MemberDescriptor>( results.values() );
	}

	private void collectMembersWithExplicitAccessAnnotation(
			LinkedHashMap<String, MemberDescriptor> attributeMemberMap,
			Set<String> transientFieldNames,
			Set<String> transientMethodNames,
			JavaTypeDescriptor managedTypeDescriptor,
			AccessType classLevelAccessType,
			LocalBindingContext localBindingContext) {
		final List<AnnotationInstance> accessAnnotations = managedTypeDescriptor.getJandexClassInfo()
				.annotations()
				.get( JPADotNames.ACCESS );
		if ( accessAnnotations == null ) {
			return;
		}

		// iterate over all @Access annotations defined on the current class
		for ( AnnotationInstance accessAnnotation : accessAnnotations ) {
			// we are only interested at annotations defined on fields and methods
			AnnotationTarget annotationTarget = accessAnnotation.target();
			if ( !annotationTarget.getClass().equals( MethodInfo.class )
					&& !annotationTarget.getClass().equals( FieldInfo.class ) ) {
				continue;
			}

			final AccessType attributeAccessType = localBindingContext.getJandexAccess()
					.getTypedValueExtractor( AccessType.class )
					.extract( accessAnnotation, "value" );
			checkExplicitJpaAttributeAccessAnnotationPlacedCorrectly(
					annotationTarget,
					attributeAccessType,
					classLevelAccessType,
					managedTypeDescriptor,
					localBindingContext
			);

			if ( annotationTarget instanceof MethodInfo ) {
				final MethodInfo methodInfo = ( (MethodInfo) annotationTarget );
				if ( transientMethodNames.contains( methodInfo.name() ) ) {
					continue;
				}

				final String attributeName = ReflectHelper.getPropertyNameFromGetterMethod( methodInfo.name() );
				if ( attributeName == null ) {
					throw localBindingContext.makeMappingException(
							"@Access annotation encountered on method [" + methodInfo.name()
									+ "] that did not following JavaBeans naming convention for getter"
					);
				}

				if ( attributeMemberMap.containsKey( attributeName ) ) {
					continue;
				}

				attributeMemberMap.put(
						attributeName,
						findMethodDescriptor( managedTypeDescriptor, methodInfo.name() )
				);
			}
			else {
				final FieldInfo fieldInfo = (FieldInfo) annotationTarget;
				final String attributeName = fieldInfo.name();
				if ( transientFieldNames.contains( attributeName ) ) {
					continue;
				}

				if ( attributeMemberMap.containsKey( attributeName ) ) {
					continue;
				}

				attributeMemberMap.put(
						attributeName,
						findFieldDescriptor( managedTypeDescriptor, attributeName )
				);
			}
		}

	}

	private void checkExplicitJpaAttributeAccessAnnotationPlacedCorrectly(
			AnnotationTarget annotationTarget,
			AccessType attributeAccessType,
			AccessType classLevelAccessType,
			JavaTypeDescriptor managedTypeDescriptor,
			LocalBindingContext localBindingContext) {

		if ( AccessType.FIELD.equals( classLevelAccessType ) ) {
			// The class-level AccessType is FIELD....

			if ( FieldInfo.class.isInstance( annotationTarget ) ) {
				// we have an @Access annotation defined on a field..
				//
				// Technically this is ok as long as the attribute-level AccessType defines
				// FIELD.  This falls under the 2.3.2 section footnote stating:
				// 		"It is permitted (but redundant) to place Access(FIELD) on a
				// 		persistent field whose class has field access type..."
				if ( AccessType.PROPERTY.equals( attributeAccessType ) ) {
					throw localBindingContext.makeMappingException(
							String.format(
									Locale.ENGLISH,
									"Illegal attempt to specify Access(PROPERTY) on field [%s] in class [%s]",
									( (FieldInfo) annotationTarget ).name(),
									( (FieldInfo) annotationTarget ).declaringClass().name()
							)
					);
				}
			}
			else {
				// we have an @Access annotation defined on a method...
				// 		(getter checks are done in the caller)
				if ( AccessType.FIELD.equals( attributeAccessType ) ) {
					throw localBindingContext.makeMappingException(
							LOG.accessTypeOverrideShouldBeProperty( managedTypeDescriptor.getName().toString() )
					);
				}
			}
		}
		else if ( AccessType.PROPERTY.equals( classLevelAccessType ) ) {
			// The class-level AccessType is PROPERTY....

			if ( MethodInfo.class.isInstance( annotationTarget ) ) {
				// we have an @Access annotation defined on a method..
				// 		(again, getter checks are done in the caller)
				//
				// Technically this is ok as long as the attribute-level AccessType defines
				// PROPERTY.  This falls under the 2.3.2 section footnote stating:
				// 		"It is permitted (but redundant) to place ... Access(PROPERTY)
				// 		on a persistent property whose class has property access type"
				if ( AccessType.FIELD.equals( attributeAccessType ) ) {
					throw localBindingContext.makeMappingException(
							String.format(
									Locale.ENGLISH,
									"Illegal attempt to specify Access(FIELD) on method [%s] in class [%s]",
									( (MethodInfo) annotationTarget ).name(),
									( (MethodInfo) annotationTarget ).declaringClass().name()
							)
					);
				}
			}
			else {
				// we have an @Access annotation defined on a field...
				if ( AccessType.PROPERTY.equals( attributeAccessType ) ) {
					throw localBindingContext.makeMappingException(
							LOG.accessTypeOverrideShouldBeField( managedTypeDescriptor.getName().toString() )
					);
				}
			}
		}
	}

	private void collectMembersUsingClassDefinedAccess(
			LinkedHashMap<String, MemberDescriptor> attributeMemberMap,
			Set<String> transientFieldNames,
			Set<String> transientMethodNames,
			JavaTypeDescriptor managedTypeDescriptor,
			AccessType classLevelAccessType,
			LocalBindingContext localBindingContext) {
		if ( AccessType.FIELD.equals( classLevelAccessType ) ) {
			for ( FieldDescriptor field : managedTypeDescriptor.getDeclaredFields() ) {
				if ( !isPersistable( field ) ) {
					continue;
				}

				final String attributeName = field.getName();

				if ( transientFieldNames.contains( attributeName ) ) {
					continue;
				}

				if ( attributeMemberMap.containsKey( attributeName ) ) {
					continue;
				}

				attributeMemberMap.put( attributeName, field );
			}
		}
		else {
			for ( MethodDescriptor method : managedTypeDescriptor.getDeclaredMethods() ) {
				if ( !isPersistable( method ) ) {
					continue;
				}

				if ( transientMethodNames.contains( method.getName() ) ) {
					continue;
				}

				final String attributeName = ReflectHelper.getPropertyNameFromGetterMethod( method.getName() );

				if ( attributeMemberMap.containsKey( attributeName ) ) {
					continue;
				}

				attributeMemberMap.put( attributeName, method );
			}
		}
	}
}
