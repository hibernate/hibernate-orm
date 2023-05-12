/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import java.util.Locale;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.MethodDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging;

import jakarta.persistence.Transient;
import net.bytebuddy.description.method.MethodDescription;

import static net.bytebuddy.description.type.TypeDescription.VOID;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class MethodDetailsImpl extends AbstractAnnotationTarget implements MethodDetails {
	private final MethodDescription methodDescriptor;
	private final ClassDetails type;
	private final MethodKind methodKind;

	private final String toString;

	public MethodDetailsImpl(
			MethodDescription methodDescriptor,
			ClassDetails type,
			MethodKind methodKind) {
		super( methodDescriptor.getDeclaredAnnotations() );

		MODEL_SOURCE_LOGGER.debugf( "Creating MethodDetails(%s#%s)", methodDescriptor.getDeclaringType().getActualName(), methodDescriptor.getName() );

		this.methodDescriptor = methodDescriptor;
		this.type = type;
		this.methodKind = methodKind;

		this.toString = String.format(
				Locale.ROOT,
				"MethodDetails(%s#%s : %s)",
				methodDescriptor.getDeclaringType().getActualName(),
				methodDescriptor.getName(),
				type.getName()
		);
	}

	@Override
	public String getName() {
		return methodDescriptor.getName();
	}

	@Override
	public ClassDetails getType() {
		return type;
	}

	@Override
	public MethodKind getMethodKind() {
		return methodKind;
	}

	@Override
	@SuppressWarnings("RedundantIfStatement")
	public boolean isPersistable() {
		if ( methodDescriptor.isStatic() ) {
			return false;
		}

		if ( hasAnnotation( Transient.class ) ) {
			return false;
		}

		if ( methodDescriptor.isSynthetic() ) {
			return false;
		}

		// only a getter can be the backing for a persistent attribute
		// in terms of where we look for annotations
		if ( methodDescriptor.getReturnType().asErasure().equals( VOID )
				|| !methodDescriptor.getParameters().isEmpty() ) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return toString;
	}
}
