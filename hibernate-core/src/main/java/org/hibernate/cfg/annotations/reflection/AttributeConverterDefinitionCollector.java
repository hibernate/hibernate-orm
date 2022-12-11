/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations.reflection;

import org.hibernate.Remove;
import org.hibernate.boot.AttributeConverterInfo;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;

/**
 * @author Steve Ebersole
 *
 * @deprecated no longer used, use {@link ConverterRegistry}
 */
@Deprecated(since = "6.2", forRemoval = true) @Remove
public interface AttributeConverterDefinitionCollector extends ConverterRegistry {
	void addAttributeConverter(AttributeConverterInfo info);
}
