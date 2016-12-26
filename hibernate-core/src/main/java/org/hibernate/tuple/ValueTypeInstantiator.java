package org.hibernate.tuple;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.InstantiationException;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

public class ValueTypeInstantiator {

	private final Class mappedClass;
	private final Constructor constructor;
	private final Map<String, Integer> constructorParameterNameToIndex;

	private ValueTypeInstantiator(
			Class mappedClass,
			Constructor constructor,
			Map<String, Integer> constructorParameterNameToIndex) {

		if ( !constructor.isAccessible() ) {
			constructor.setAccessible( true );
		}

		this.mappedClass = mappedClass;
		this.constructor = constructor;
		this.constructorParameterNameToIndex = constructorParameterNameToIndex;
	}

	public static Optional<ValueTypeInstantiator> of(Component component) {

		Map<String, Class> propertyNameToType = component.getPropertyStream()
				.collect( Collectors.toMap( Property::getName, p -> p.getType().getReturnedClass() ) );

		Class componentClass = component.getComponentClass();
		return Stream.of( componentClass.getDeclaredConstructors() )
				.filter( c -> parametersMatchProperties( c.getParameters(), propertyNameToType ) )
				.findFirst()
				.map( c -> new ValueTypeInstantiator( componentClass, c, getParameterNameToIndex( c ) ) );
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

	private static Map<String, Integer> getParameterNameToIndex(Constructor constructor) {

		Parameter[] parameters = constructor.getParameters();
		Map<String, Integer> constructorParameterNameToIndex = new HashMap<>();

		for ( int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++ ) {
			Parameter parameter = parameters[parameterIndex];
			constructorParameterNameToIndex.put( parameter.getName(), parameterIndex );
		}

		return constructorParameterNameToIndex;
	}
}
