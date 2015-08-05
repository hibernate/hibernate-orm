/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Field;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PropertyAccessFieldImpl implements PropertyAccess {
	private final PropertyAccessStrategyFieldImpl strategy;
	private final Getter getter;
	private final Setter setter;

	public PropertyAccessFieldImpl(
			PropertyAccessStrategyFieldImpl strategy,
			Class containerJavaType,
			final String propertyName) {
		this.strategy = strategy;

		final Field field = ReflectHelper.findField( containerJavaType, propertyName );
		this.getter = new GetterFieldImpl( containerJavaType, propertyName, field );
		this.setter = new SetterFieldImpl( containerJavaType, propertyName, field );
	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return strategy;
	}

	@Override
	public Getter getGetter() {
		return getter;
	}

	@Override
	public Setter getSetter() {
		return setter;
	}

}
