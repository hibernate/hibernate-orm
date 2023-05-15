/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.MethodDetails;

import net.bytebuddy.description.method.MethodDescription;

import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class MethodDetailsImpl extends AbstractAnnotationTarget implements MethodDetails {
	private final MethodDescription methodDescriptor;
	private final ClassDetails type;
	private final MethodKind methodKind;

	private final String methodNameStem;
	private final String toString;

	public MethodDetailsImpl(MethodDescription methodDescriptor, MethodKind methodKind) {
		this( methodDescriptor, null, methodKind );
	}

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
				type == null ? "???" : type.getName()
		);

		if ( methodKind == MethodKind.GETTER ) {
			if ( methodDescriptor.getName().startsWith( "get" )
					|| methodDescriptor.getName().startsWith( "has" ) ) {
				methodNameStem = methodDescriptor.getName().substring( 3, methodDescriptor.getName().length() - 1 );
			}
			else if ( methodDescriptor.getName().startsWith( "is" ) ) {
				methodNameStem = methodDescriptor.getName().substring( 2, methodDescriptor.getName().length() - 1 );
			}
			else {
				throw new HibernateException( "Could not determine attribute method name stem for getter method - " + methodDescriptor.getName() );
			}
		}
		else if ( methodKind == MethodKind.SETTER ) {
			if ( methodDescriptor.getName().startsWith( "set" ) ) {
				methodNameStem = methodDescriptor.getName().substring( 3, methodDescriptor.getName().length() - 1 );
			}
			else {
				throw new HibernateException( "Could not determine attribute method name stem for setter method - " + methodDescriptor.getName() );
			}
		}
		else {
			methodNameStem = null;
		}
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
	public String resolveAttributeMethodNameStem() {
		return methodNameStem;
	}

	@Override
	public String toString() {
		return toString;
	}
}
