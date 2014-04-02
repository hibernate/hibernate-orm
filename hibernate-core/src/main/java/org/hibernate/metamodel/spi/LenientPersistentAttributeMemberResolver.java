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
import java.util.Set;
import javax.persistence.AccessType;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

/**
 * PersistentAttributeMemberResolver implementation that is more lenient in some cases
 * than the JPA specification with the idea of being more user friendly.
 *
 * @author Steve Ebersole
 */
public class LenientPersistentAttributeMemberResolver extends AbstractPersistentAttributeMemberResolver {
	private static final Logger LOG = Logger.getLogger( LenientPersistentAttributeMemberResolver.class );

	/**
	 * Singleton access
	 */
	public static final LenientPersistentAttributeMemberResolver INSTANCE = new LenientPersistentAttributeMemberResolver();

	@Override
	protected List<MemberDescriptor> resolveAttributesMembers(
			Set<String> transientFieldNames,
			Set<String> transientMethodNames,
			JavaTypeDescriptor managedTypeDescriptor,
			AccessType classLevelAccessType,
			LocalBindingContext localBindingContext) {
		final LinkedHashMap<String,MemberDescriptor> attributeMemberMap = new LinkedHashMap<String, MemberDescriptor>();

		collectAnnotatedMembers(
				attributeMemberMap,
				transientFieldNames,
				transientMethodNames,
				managedTypeDescriptor
		);

		collectNonAnnotatedMembers(
				attributeMemberMap,
				transientFieldNames,
				transientMethodNames,
				managedTypeDescriptor
		);

		return new ArrayList<MemberDescriptor>( attributeMemberMap.values() );
	}

	private void collectAnnotatedMembers(
			LinkedHashMap<String,MemberDescriptor> attributeMemberMap,
			Set<String> transientFieldNames,
			Set<String> transientMethodNames,
			JavaTypeDescriptor managedTypeDescriptor) {
		for ( List<AnnotationInstance> annotationInstances :
				managedTypeDescriptor.getJandexClassInfo().annotations().values() ) {

			for ( AnnotationInstance annotationInstance : annotationInstances ) {
				final String annotationTypeName = annotationInstance.name().toString();
				if ( !annotationTypeName.startsWith( "javax.persistence." )
						&& !annotationTypeName.startsWith( "org.hibernate.annotations." ) ) {
					continue;
				}

				final AnnotationTarget target = annotationInstance.target();
				if ( FieldInfo.class.isInstance( target ) ) {
					final FieldInfo field = (FieldInfo) target;

					if ( transientFieldNames.contains( field.name() ) ) {
						continue;
					}

					final MemberDescriptor existing = attributeMemberMap.get( field.name() );
					if ( existing != null ) {
						if ( !FieldDescriptor.class.isInstance( existing ) ) {
							LOG.warnf(
									"Found annotations split between field [%s] and method [%s]",
									field.name(),
									existing.getName()
							);
						}
						continue;
					}

					attributeMemberMap.put(
							field.name(),
							findFieldDescriptor( managedTypeDescriptor, field.name() )
					);
				}
				else if ( MethodInfo.class.isInstance( target ) ) {
					final MethodInfo method = (MethodInfo) target;
					if ( transientMethodNames.contains( method.name() ) ) {
						continue;
					}

					final String attributeName = ReflectHelper.getPropertyNameFromGetterMethod( method.name() );
					final MemberDescriptor existing = attributeMemberMap.get( attributeName );

					if ( existing != null ) {
						if ( !MethodDescriptor.class.isInstance( existing ) ) {
							LOG.warnf(
									"Found annotations split between field [%s] and method [%s]",
									existing.getName(),
									method.name()
							);
						}
						continue;
					}

					attributeMemberMap.put(
							attributeName,
							findMethodDescriptor( managedTypeDescriptor, method.name() )
					);
				}
			}
		}
	}

	private void collectNonAnnotatedMembers(
			LinkedHashMap<String, MemberDescriptor> attributeMemberMap,
			Set<String> transientFieldNames,
			Set<String> transientMethodNames,
			JavaTypeDescriptor managedTypeDescriptor) {
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

		for ( FieldDescriptor field : managedTypeDescriptor.getDeclaredFields() ) {
			if ( !isPersistable( field ) ) {
				continue;
			}

			if ( transientFieldNames.contains( field.getName() ) ) {
				continue;
			}

			if ( attributeMemberMap.containsKey( field.getName() ) ) {
				continue;
			}

			attributeMemberMap.put( field.getName(), field );
		}
	}
}
