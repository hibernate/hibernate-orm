/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorMappingsImplicitImpl implements DiscriminatorMappings {
	/**
	 * Singleton access
	 */
	public static final DiscriminatorMappingsImplicitImpl INSTANCE = new DiscriminatorMappingsImplicitImpl();

	@Override
	public String discriminatorValueToEntityName(Object discriminatorValue) {
		return (String) discriminatorValue;
	}

	@Override
	public Object entityNameToDiscriminatorValue(String discriminatorValue) {
		return discriminatorValue;
	}
}
