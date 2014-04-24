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

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.AccessType;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.reflite.internal.ModifierUtils;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.VoidDescriptor;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

/**
 * "Template" support for writing PersistentAttributeMemberResolver
 * implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPersistentAttributeMemberResolver implements PersistentAttributeMemberResolver {

	/**
	 * This is the call that represents the bulk of the work needed to resolve
	 * the persistent attribute members.  It is the strategy specific portion
	 * for sure.
	 * <p/>
	 * The expectation is to
	 * Here is the call that most likely changes per strategy.  This occurs
	 * immediately after we have determined all the fields and methods marked as
	 * transient.  The expectation is to
	 *
	 * @param transientFieldNames The set of all field names found to have been
	 * annotated as @Transient
	 * @param transientMethodNames The set of all method names found to have been
	 * annotated as @Transient
	 * @param managedTypeDescriptor The ManagedType Java type descriptor
	 * @param classLevelAccessType The AccessType determined for the class default
	 * @param localBindingContext The local binding context
	 */
	protected abstract List<MemberDescriptor> resolveAttributesMembers(
			Set<String> transientFieldNames,
			Set<String> transientMethodNames,
			JavaTypeDescriptor managedTypeDescriptor,
			AccessType classLevelAccessType,
			LocalBindingContext localBindingContext);

	@Override
	public List<MemberDescriptor> resolveAttributesMembers(
			JavaTypeDescriptor managedTypeDescriptor,
			AccessType classLevelAccessType,
			LocalBindingContext localBindingContext) {

		final Set<String> transientFieldNames = new HashSet<String>();
		final Set<String> transientMethodNames = new HashSet<String>();
		collectMembersMarkedTransient(
				transientFieldNames,
				transientMethodNames,
				managedTypeDescriptor,
				localBindingContext
		);

		return resolveAttributesMembers(
				transientFieldNames,
				transientMethodNames,
				managedTypeDescriptor,
				classLevelAccessType,
				localBindingContext
		);
	}

	protected void collectMembersMarkedTransient(
			Set<String> transientFieldNames,
			Set<String> transientMethodNames,
			JavaTypeDescriptor managedTypeDescriptor,
			LocalBindingContext localBindingContext) {
		List<AnnotationInstance> transientMembers = managedTypeDescriptor.getJandexClassInfo()
				.annotations()
				.get( JPADotNames.TRANSIENT );
		if ( transientMembers == null ) {
			return;
		}

		for ( AnnotationInstance transientMember : transientMembers ) {
			AnnotationTarget target = transientMember.target();

			// todo : would could limit these to "persistable" fields/methods, but not sure its worth the processing to check..

			if ( target instanceof FieldInfo ) {
				transientFieldNames.add( ( (FieldInfo) target ).name() );
			}
			else if ( target instanceof MethodInfo ) {
				transientMethodNames.add( ( (MethodInfo) target ).name() );
			}
			else {
				throw localBindingContext.makeMappingException(
						"@Transient should only be defined on field or method : " + target
				);
			}
		}
	}


	protected FieldDescriptor findFieldDescriptor(JavaTypeDescriptor javaTypeDescriptor, String name) {
		for ( FieldDescriptor fieldDescriptor : javaTypeDescriptor.getDeclaredFields() ) {
			// perform a series of opt-out checks
			if ( ! fieldDescriptor.getName().equals( name ) ) {
				continue;
			}

			if ( ! isPersistable( fieldDescriptor ) ) {
				continue;
			}

			// no opt-outs above, we found it...
			return fieldDescriptor;
		}

		throw new HibernateException(
				"Unable to locate persistent field [" + name + "] - class " +
						javaTypeDescriptor.getName().toString()
		);
	}

	@SuppressWarnings("RedundantIfStatement")
	public static boolean isPersistable(FieldDescriptor fieldDescriptor) {
		if ( Modifier.isTransient( fieldDescriptor.getModifiers() ) ) {
			return false;
		}

		if ( ModifierUtils.isSynthetic( fieldDescriptor ) ) {
			return false;
		}

		return true;
	}

	protected MethodDescriptor findMethodDescriptor(JavaTypeDescriptor javaTypeDescriptor, String name) {
		for ( MethodDescriptor methodDescriptor : javaTypeDescriptor.getDeclaredMethods() ) {
			// perform a series of opt-out checks
			if ( !methodDescriptor.getName().equals( name ) ) {
				continue;
			}

			if ( !isPersistable( methodDescriptor ) ) {
				continue;
			}

			// no opt-outs above, we found it...
			return methodDescriptor;
		}

		throw new HibernateException(
				"Unable to locate persistent property method [" + name + "] - class " +
						javaTypeDescriptor.getName().toString()
		);
	}

	@SuppressWarnings("RedundantIfStatement")
	public static boolean isPersistable(MethodDescriptor methodDescriptor) {
		if ( !methodDescriptor.getArgumentTypes().isEmpty() ) {
			return false;
		}

		if ( methodDescriptor.getReturnType() == null
				|| methodDescriptor.getReturnType().getErasedType() == VoidDescriptor.INSTANCE ) {
			return false;
		}

		if ( !methodDescriptor.getName().startsWith( "get" )
				&& !methodDescriptor.getName().startsWith( "is"  ) ) {
			return false;
		}

		if ( Modifier.isStatic( methodDescriptor.getModifiers() ) ) {
			return false;
		}

		if ( ModifierUtils.isBridge( methodDescriptor ) ) {
			return false;
		}

		if ( ModifierUtils.isSynthetic( methodDescriptor ) ) {
			return false;
		}

		return true;
	}
}
