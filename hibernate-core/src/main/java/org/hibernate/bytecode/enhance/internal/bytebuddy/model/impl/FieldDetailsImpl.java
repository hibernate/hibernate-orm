/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import java.util.Locale;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.FieldDetails;
import org.hibernate.internal.util.StringHelper;

import net.bytebuddy.description.field.FieldDescription;

import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class FieldDetailsImpl extends AbstractAnnotationTarget implements FieldDetails {
	private final FieldDescription fieldDescriptor;
	private final ClassDetails type;

	private final String methodNameStem;
	private final String toString;

	public FieldDetailsImpl(FieldDescription fieldDescriptor, ClassDetails type) {
		super( fieldDescriptor.getDeclaredAnnotations() );

		MODEL_SOURCE_LOGGER.debugf( "Creating FieldDetails(%s#%s)", fieldDescriptor.getDeclaringType().getActualName(), fieldDescriptor.getName() );

		this.fieldDescriptor = fieldDescriptor;
		this.type = type;

		this.toString = String.format(
				Locale.ROOT,
				"MethodDetails(%s#%s : %s)",
				fieldDescriptor.getDeclaringType().getActualName(),
				fieldDescriptor.getName(),
				type.getName()
		);
		this.methodNameStem = StringHelper.capitalizeFirst( fieldDescriptor.getName() );
	}

	@Override
	public String getName() {
		return fieldDescriptor.getName();
	}

	@Override
	public ClassDetails getType() {
		return type;
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
