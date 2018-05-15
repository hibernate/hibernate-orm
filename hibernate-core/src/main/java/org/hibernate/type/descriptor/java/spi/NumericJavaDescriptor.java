/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public interface NumericJavaDescriptor<T extends Serializable> extends BasicJavaDescriptor<T> {
	// todo (6.0) : ? - implement these?
//	Byte toByte(Boolean value);
//	Short toShort(Boolean value);
//	Integer toInteger(Boolean value);
//	Long toLong(Boolean value);
}
