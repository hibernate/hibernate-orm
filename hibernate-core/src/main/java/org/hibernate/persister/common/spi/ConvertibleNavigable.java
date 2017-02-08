/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.Optional;
import javax.persistence.AttributeConverter;

import org.hibernate.type.converter.spi.AttributeConverterDefinition;

/**
 * Specialization of a DomainReference whose values may have a JPA
 * {@link AttributeConverter} applied.
 *
 * @author Steve Ebersole
 */
public interface ConvertibleNavigable<T> extends Navigable<T> {
	Optional<AttributeConverterDefinition> getAttributeConverter();
}
