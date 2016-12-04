package org.hibernate.tuple;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.InstantiationException;
import org.hibernate.mapping.Component;

public class ValueTypeInstantiator {

	private final Class mappedClass;
	private final Constructor constructor;
	private final Map<String, Integer> constructorParameterNameToIndex;

	private ValueTypeInstantiator(
			Class mappedClass,
			Constructor constructor,
			Map<String, Integer> constructorParameterNameToIndex) {
		this.mappedClass = mappedClass;
		this.constructor = constructor;
		this.constructorParameterNameToIndex = constructorParameterNameToIndex;
	}

	public static Optional<ValueTypeInstantiator> of(Component component) {

		Class componentClass = component.getComponentClass();
		Map<String, Class> propertyNameToType = new HashMap<>();
		component.getPropertyIterator()
				.forEachRemaining( property -> propertyNameToType.put(
						property.getName(),
						property.getType().getReturnedClass()
				) );

		Constructor constructor = Stream.of( componentClass.getDeclaredConstructors() )
				.filter( c -> parametersMatchProperties( c.getParameters(), propertyNameToType ) )
				.findFirst()
				.orElse( null );

		if ( constructor == null ) {
			return Optional.empty();
		}

		Parameter[] parameters = constructor.getParameters();
		Map<String, Integer> constructorParameterNameToIndex = new HashMap<>();

		for ( int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++ ) {
			Parameter parameter = parameters[parameterIndex];
			constructorParameterNameToIndex.put( parameter.getName(), parameterIndex );
		}

		if ( !constructor.isAccessible() ) {
			constructor.setAccessible( true );
		}

		return Optional.of( new ValueTypeInstantiator( componentClass, constructor, constructorParameterNameToIndex ) );
	}

	public Object instantiate(String[] propertyNames, Object[] values) {
		Object[] arguments = new Object[values.length];
		for ( int valueIndex = 0; valueIndex < values.length; valueIndex++ ) {
			Object value = values[valueIndex];
			Integer argumentIndex = constructorParameterNameToIndex.get( propertyNames[valueIndex] );
			arguments[argumentIndex] = value;
		}
		try {
			return constructor.newInstance( arguments );
		}
		catch (Exception e) {
			throw new InstantiationException( "Could not instantiate entity: ", mappedClass, e );
		}
	}

	public boolean isInstance(Object object) {
		return mappedClass.isInstance( object );
	}

	private static boolean parametersMatchProperties(Parameter[] parameters, Map<String, Class> propertyNameToType) {

		if ( parameters.length != propertyNameToType.size() ) {
			return false;
		}

		for ( Parameter parameter : parameters ) {
			Class propertyType = propertyNameToType.get( parameter.getName() );
			if ( propertyType == null || !Objects.equals( parameter.getType(), propertyType ) ) {
				return false;
			}
		}

		return true;
	}
}
