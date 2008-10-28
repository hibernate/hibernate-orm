package org.hibernate.annotations.common.reflection.java;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;

/**
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
class JavaXProperty extends JavaXMember implements XProperty {

	static JavaXProperty create(Member member, final TypeEnvironment context, final JavaReflectionManager factory) {
		final Type propType = typeOf( member, context );
		JavaXType xType = factory.toXType( context, propType );
		return new JavaXProperty( member, propType, context, factory, xType );
	}

	private JavaXProperty(Member member, Type type, TypeEnvironment env, JavaReflectionManager factory, JavaXType xType) {
		super( member, type, env, factory, xType );
		assert member instanceof Field || member instanceof Method;
	}

	public String getName() {
		String fullName = getMember().getName();
		if ( getMember() instanceof Method ) {
			if ( fullName.startsWith( "get" ) ) {
				return Introspector.decapitalize( fullName.substring( "get".length() ) );
			}
			if ( fullName.startsWith( "is" ) ) {
				return Introspector.decapitalize( fullName.substring( "is".length() ) );
			}
			throw new RuntimeException( "Method " + fullName + " is not a property getter" );
		}
		else {
			return fullName;
		}
	}

	public Object invoke(Object target, Object... parameters) {
		if ( parameters.length != 0 ) {
			throw new IllegalArgumentException( "An XProperty cannot have invoke parameters" );
		}
		try {
			if ( getMember() instanceof Method ) {
				return ( (Method) getMember() ).invoke( target );
			}
			else {
				return ( (Field) getMember() ).get( target );
			}
		}
		catch (NullPointerException e) {
			throw new IllegalArgumentException( "Invoking " + getName() + " on a  null object", e );
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException( "Invoking " + getName() + " with wrong parameters", e );
		}
		catch (Exception e) {
			throw new IllegalStateException( "Unable to invoke " + getName(), e );
		}
	}

	@Override
	public String toString() {
		return getName();
	}
}