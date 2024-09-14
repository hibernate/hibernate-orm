/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.build.annotations;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;

/**
 * @author Steve Ebersole
 */
public class ClassFileHelper {
	public static final String GENERATION_PACKAGE = "org.hibernate.boot.models.annotations.spi";
	static final Pattern CAMEL_CASE_SPLITTER = Pattern.compile( "(([A-Z]?[a-z]+)|([A-Z]))" );

	static String determineConstantName(TypeElement annotationClass) {
		final StringBuilder nameBuffer = new StringBuilder();
		final Matcher matcher = CAMEL_CASE_SPLITTER.matcher( annotationClass.getSimpleName().toString() );
		boolean firstPass = true;
		while ( matcher.find() ) {
			if ( !firstPass ) {
				nameBuffer.append( '_' );
			}
			else {
				firstPass = false;
			}
			nameBuffer.append( matcher.group(0).toUpperCase( Locale.ROOT ) );
		}
		return nameBuffer.toString();
	}

	public static Object defaultValueValue(Method declaredMethod) {
		// should not get in here if there is no default
		assert declaredMethod.getDefaultValue() != null;

		if ( declaredMethod.getReturnType().isAnnotation() ) {
			return String.format(
					Locale.ROOT,
					"modelContext.getAnnotationDescriptorRegistry().getDescriptor(%s.class).createUsage(modelContext)",
					declaredMethod.getReturnType().getName()
			);
		}

		if ( String.class.equals( declaredMethod.getReturnType() ) ) {
			return "\"" + declaredMethod.getDefaultValue() + "\"";
		}

		if ( long.class.equals( declaredMethod.getReturnType() ) ) {
			return declaredMethod.getDefaultValue() + "L";
		}

		if ( declaredMethod.getReturnType().isArray() ) {
			final Class<?> componentType = declaredMethod.getReturnType().getComponentType();
			return "new " + componentType.getName() + "[0]";
		}

		return declaredMethod.getDefaultValue();
	}
}
