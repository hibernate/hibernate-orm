/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

import jakarta.persistence.AttributeConverter;

/**
 * Marker for Hibernate supplied {@linkplain AttributeConverter converter} classes.
 * <p/>
 * Also implements the Hibernate-specific BasicValueConverter contract
 *
 * @author Steve Ebersole
 */
public interface StandardConverter<O,R> extends AttributeConverter<O,R>, BasicValueConverter<O,R> {
}
