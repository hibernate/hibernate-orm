/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

import jakarta.annotation.Nullable;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.Property;
import org.hibernate.accessor.HibernateAccessorFactory;
import org.hibernate.accessor.HibernateAccessorInstantiator;
import org.hibernate.accessor.HibernateAccessorMultiValueReader;
import org.hibernate.accessor.HibernateAccessorMultiValueWriter;
import org.hibernate.property.access.internal.PropertyAccessEmbeddedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyIndexBackRefImpl;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.property.access.spi.SetterFieldImpl;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

class PropertyAccessHelper {

	static PropertyAccessStrategy propertyAccessStrategy(
			Property bootAttributeDescriptor,
			Class<?> mappedClass,
			StrategySelector strategySelector) {
		final var strategy = bootAttributeDescriptor.getPropertyAccessStrategy( mappedClass );
		if ( strategy != null ) {
			return strategy;
		}
		else {
			final String propertyAccessorName = bootAttributeDescriptor.getPropertyAccessorName();
			if ( isNotEmpty( propertyAccessorName ) ) {
				// handle explicitly specified attribute accessor
				return strategySelector.resolveStrategy( PropertyAccessStrategy.class, propertyAccessorName );
			}
			else {
				if ( bootAttributeDescriptor instanceof Backref backref ) {
					return new PropertyAccessStrategyBackRefImpl(
							backref.getCollectionRole(),
							backref.getEntityName()
					);
				}
				else if ( bootAttributeDescriptor instanceof IndexBackref indexBackref ) {
					return new PropertyAccessStrategyIndexBackRefImpl(
							indexBackref.getCollectionRole(),
							indexBackref.getEntityName()
					);
				}
				else {
					// for now...
					return BuiltInPropertyAccessStrategies.MIXED.getStrategy();
				}
			}
		}
	}

	static @Nullable HibernateAccessorInstantiator<?> resolveInstantiator(
			Class<?> clazz,
			PropertyAccessorService accessorService) {
		if ( clazz.isInterface() || Modifier.isAbstract( clazz.getModifiers() ) ) {
			return null;
		}
		try {
			final Constructor<?> constructor = clazz.getDeclaredConstructor();
			if ( Modifier.isPrivate( constructor.getModifiers() ) ) {
				return null;
			}
			return accessorService.hibernateAccessorFactory().instantiator( constructor );
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	record MultiValueAccessors(
			@Nullable HibernateAccessorMultiValueReader reader,
			@Nullable HibernateAccessorMultiValueWriter writer) {
		static final MultiValueAccessors NONE = new MultiValueAccessors( null, null );
	}

	static MultiValueAccessors buildMultiValueAccessors(
			HibernateAccessorFactory factory,
			Class<?> clazz,
			Collection<PropertyAccess> propertyAccesses) {
		final var getterMembers = extractGetterMembers( propertyAccesses );
		if ( getterMembers == null ) {
			return MultiValueAccessors.NONE;
		}
		final var setterMembers = extractSetterMembers( propertyAccesses );
		final var reader = tryMultiValueReader( factory, clazz, getterMembers );
		final var writer = setterMembers != null
				? tryMultiValueWriter( factory, clazz, setterMembers )
				: null;
		return new MultiValueAccessors( reader, writer );
	}

	private static @Nullable HibernateAccessorMultiValueReader tryMultiValueReader(
			HibernateAccessorFactory factory, Class<?> clazz, Member[] members) {
		try {
			return factory.multiValueReader( clazz, members );
		}
		catch (Exception e) {
			return null;
		}
	}

	private static @Nullable HibernateAccessorMultiValueWriter tryMultiValueWriter(
			HibernateAccessorFactory factory, Class<?> clazz, Member[] members) {
		try {
			return factory.multiValueWriter( clazz, members );
		}
		catch (Exception e) {
			return null;
		}
	}

	private static @Nullable Member[] extractGetterMembers(Collection<PropertyAccess> propertyAccesses) {
		final var members = new Member[propertyAccesses.size()];
		int i = 0;
		for ( PropertyAccess propertyAccess : propertyAccesses ) {
			if ( propertyAccess instanceof PropertyAccessEmbeddedImpl ) {
				return null;
			}
			final Getter getter = propertyAccess.getGetter();
			if ( getter == null ) {
				return null;
			}
			if ( getter instanceof GetterFieldImpl getterField ) {
				members[i++] = getterField.getField();
			}
			else {
				final Member member = getter.getMember();
				if ( member == null ) {
					return null;
				}
				members[i++] = member;
			}
		}
		return members;
	}

	private static @Nullable Member[] extractSetterMembers(Collection<PropertyAccess> propertyAccesses) {
		final var members = new Member[propertyAccesses.size()];
		int i = 0;
		for ( PropertyAccess propertyAccess : propertyAccesses ) {
			final var setter = propertyAccess.getSetter();
			if ( setter instanceof SetterFieldImpl setterField ) {
				members[i] = setterField.getField();
			}
			else {
				final Method method = setter != null ? setter.getMethod() : null;
				if ( method == null ) {
					return null;
				}
				members[i] = method;
			}
			i++;
		}
		return members;
	}
}
