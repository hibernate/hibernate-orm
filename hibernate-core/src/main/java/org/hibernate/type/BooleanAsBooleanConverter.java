/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.type;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Simple pass-through boolean value converter.
 * Useful in {@linkplain SoftDelete#converter() certain scenarios}.
 *
 * @author Steve Ebersole
 */
public class BooleanAsBooleanConverter implements StandardBooleanConverter<Boolean> {
	public static final BooleanAsBooleanConverter INSTANCE = new BooleanAsBooleanConverter();

	@Override
	public Boolean convertToDatabaseColumn(Boolean attribute) {
		return toRelationalValue( attribute );
	}

	@Override
	public Boolean convertToEntityAttribute(Boolean dbData) {
		return toDomainValue( dbData );
	}

	@Override
	public Boolean toDomainValue(Boolean relationalForm) {
		return relationalForm;
	}

	@Override
	public Boolean toRelationalValue(Boolean domainForm) {
		return domainForm;
	}

	@Override
	public JavaType<Boolean> getDomainJavaType() {
		return BooleanJavaType.INSTANCE;
	}

	@Override
	public JavaType<Boolean> getRelationalJavaType() {
		return BooleanJavaType.INSTANCE;
	}
}
