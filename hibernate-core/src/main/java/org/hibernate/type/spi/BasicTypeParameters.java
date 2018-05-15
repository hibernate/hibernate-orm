/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import org.hibernate.Incubating;
import org.hibernate.annotations.Remove;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Essentially acts as a parameter-object to {@link BasicTypeRegistry#resolveBasicType}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface BasicTypeParameters<T> {
	BasicJavaDescriptor<T> getJavaTypeDescriptor();

	SqlTypeDescriptor getSqlTypeDescriptor();

	/**
	 * @deprecated 6.0 no longer implements "attribute conversion" at the BasicType level.
	 *
	 * @see SqlExpressableType
	 */
	@Remove
	@Deprecated
	ConverterDescriptor getAttributeConverterDescriptor();

	javax.persistence.TemporalType getTemporalPrecision();
}
