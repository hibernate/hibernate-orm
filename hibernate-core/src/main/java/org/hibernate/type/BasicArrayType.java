/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * A type that maps between {@link java.sql.Types#ARRAY ARRAY} and {@code T[]}
 *
 * @author Jordan Gigov
 * @author Christian Beikov
 */
public class BasicArrayType<T>
		extends AbstractSingleColumnStandardBasicType<T[]>
		implements AdjustableBasicType<T[]>, BasicPluralType<T[], T> {

	private final BasicType<T> baseDescriptor;
	private final String name;

	public BasicArrayType(BasicType<T> baseDescriptor, JdbcType arrayJdbcType, JavaType<T[]> arrayTypeDescriptor) {
		super( arrayJdbcType, arrayTypeDescriptor );
		this.baseDescriptor = baseDescriptor;
		this.name = determineArrayTypeName( baseDescriptor );
	}

	static String determineElementTypeName(BasicType<?> baseDescriptor) {
		final String elementName = baseDescriptor.getName();
		switch ( elementName ) {
			case "boolean":
			case "byte":
			case "char":
			case "short":
			case "int":
			case "long":
			case "float":
			case "double":
				return Character.toUpperCase( elementName.charAt( 0 ) ) + elementName.substring( 1 );
			default:
				return elementName;
		}
	}

	static String determineArrayTypeName(BasicType<?> baseDescriptor) {
		return determineElementTypeName( baseDescriptor ) + "[]";
	}

	@Override
	public BasicType<T> getElementType() {
		return baseDescriptor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> domainJtd) {
		// TODO: maybe fallback to some encoding by default if the DB doesn't support arrays natively?
		//  also, maybe move that logic into the ArrayJdbcType
		//noinspection unchecked
		return (BasicType<X>) this;
	}
}
