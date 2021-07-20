/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc.spi;

import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Support for JdbcLiteralFormatter implementations with a basic implementation of an unwrap method
 *
 * @author Steve Ebersole
 */
public abstract class BasicJdbcLiteralFormatter extends AbstractJdbcLiteralFormatter {
	public BasicJdbcLiteralFormatter(JavaTypeDescriptor<?> javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@SuppressWarnings("unchecked")
	protected <X> X unwrap(Object value, Class<X> unwrapType, WrapperOptions wrapperOptions) {
		assert value != null;

		// for performance reasons, avoid conversions if we can
		if ( unwrapType.isInstance( value ) ) {
			return (X) value;
		}

		if ( value instanceof SqmLiteral && ( (SqmLiteral<?>) value ).getNodeType() instanceof SqmEnumLiteral ) {
			value = ( (SqmEnumLiteral) ( (SqmLiteral) value ).getNodeType() ).getEnumValue();
			assert value != null;

			if ( unwrapType == String.class ) {
				value = ( ( Enum ) value ).name();
			}
			else {
				value = ( ( Enum ) value ).ordinal();
			}
		}

		return (X) getJavaTypeDescriptor().unwrap( value, unwrapType, wrapperOptions );
	}
}
