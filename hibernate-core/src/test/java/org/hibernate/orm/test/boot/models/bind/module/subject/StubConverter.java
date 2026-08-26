/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module.subject;

import jakarta.persistence.AttributeConverter;

public class StubConverter implements AttributeConverter<StubDomainType, String> {
	@Override
	public String convertToDatabaseColumn(StubDomainType attribute) {
		return attribute == null ? null : attribute.value;
	}

	@Override
	public StubDomainType convertToEntityAttribute(String dbData) {
		return dbData == null ? null : new StubDomainType( dbData );
	}
}
