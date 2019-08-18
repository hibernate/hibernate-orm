/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.model.domain.BasicDomainType;

/**
 * SPI extension to {@link BasicDomainType}
 *
 * @author Steve Ebersole
 */
public interface BasicTypeDescriptor<J>
		extends BasicDomainType<J>, SimpleTypeDescriptor<J>, AllowableParameterType<J>, AllowableFunctionReturnType<J> {

	@Override
	default boolean areEqual(J x, J y) throws HibernateException {
		return Objects.equals( x, y );
	}

}
