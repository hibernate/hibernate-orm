/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.converter.spi;

import javax.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public interface ConverterBuilder<O,R> {
	Class<? extends AttributeConverter<O,R>> getImplementationClass();
	AttributeConverter<O,R> buildAttributeConverter(ConverterBuildingContext context);
}