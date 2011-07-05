package org.hibernate.metamodel.binder.source.annotations;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Some helper methods for reflection tasks
 *
 * @author Hardy Ferentschik
 */
public class ReflectionHelper {

	private ReflectionHelper() {
	}

	/**
	 * Process bean properties getter by applying the JavaBean naming conventions.
	 *
	 * @param member the member for which to get the property name.
	 *
	 * @return The bean method name with the "is" or "get" prefix stripped off, {@code null}
	 *         the method name id not according to the JavaBeans standard.
	 */
	public static String getPropertyName(Member member) {
		String name = null;

		if ( member instanceof Field ) {
			name = member.getName();
		}

		if ( member instanceof Method ) {
			String methodName = member.getName();
			if ( methodName.startsWith( "is" ) ) {
				name = Introspector.decapitalize( methodName.substring( 2 ) );
			}
			else if ( methodName.startsWith( "has" ) ) {
				name = Introspector.decapitalize( methodName.substring( 3 ) );
			}
			else if ( methodName.startsWith( "get" ) ) {
				name = Introspector.decapitalize( methodName.substring( 3 ) );
			}
		}
		return name;
	}

	public static boolean isProperty(Member m) {
		if ( m instanceof Method ) {
			Method method = (Method) m;
			return !method.isSynthetic()
					&& !method.isBridge()
					&& !Modifier.isStatic( method.getModifiers() )
					&& method.getParameterTypes().length == 0
					&& ( method.getName().startsWith( "get" ) || method.getName().startsWith( "is" ) );
		}
		else {
			return !Modifier.isTransient( m.getModifiers() ) && !m.isSynthetic();
		}
	}
}


