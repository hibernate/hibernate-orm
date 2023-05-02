/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static java.sql.Types.ARRAY;

/**
 * @author Gavin King
 */
public class ArrayDdlTypeImpl extends DdlTypeImpl {

	public ArrayDdlTypeImpl(Dialect dialect) {
		super( ARRAY, "array", dialect );
	}

	@Override
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) type;
		final BasicPluralJavaType<?> javaTypeDescriptor = (BasicPluralJavaType<?>) pluralType.getJavaTypeDescriptor();
		final BasicType<?> elementType = pluralType.getElementType();
		final String arrayElementTypeName =
				ddlTypeRegistry.getTypeName(
						elementType.getJdbcType().getDdlTypeCode(),
						dialect.getSizeStrategy().resolveSize(
								elementType.getJdbcMapping().getJdbcType(),
								elementType.getJavaTypeDescriptor(),
								columnSize.getPrecision(),
								columnSize.getScale(),
								columnSize.getLength()
						),
						elementType
				);
		return dialect.getArrayTypeName(
				javaTypeDescriptor.getElementJavaType().getJavaTypeClass().getSimpleName(),
				arrayElementTypeName,
				columnSize.getArrayLength()
		);
	}

}
