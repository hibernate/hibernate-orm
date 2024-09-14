/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.converter.spi;

import jakarta.persistence.AttributeConverter;

import org.hibernate.Incubating;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * {@link BasicValueConverter} extension for {@link AttributeConverter}-specific support
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JpaAttributeConverter<O,R> extends BasicValueConverter<O,R> {
	JavaType<? extends AttributeConverter<O,R>> getConverterJavaType();

	ManagedBean<? extends AttributeConverter<O,R>> getConverterBean();
}
