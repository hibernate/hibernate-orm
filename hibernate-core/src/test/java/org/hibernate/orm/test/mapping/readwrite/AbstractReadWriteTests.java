/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.readwrite;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;

import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SessionFactory
public abstract class AbstractReadWriteTests {
	@Test
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy(
				ReadWriteEntity.class,
				(rootClass) -> {
					final Property property = rootClass.getProperty( "value" );
					final BasicValue valueMapping = (BasicValue) property.getValue();
					final Column column = (Column) valueMapping.getColumn();
					final String customRead = column.getCustomRead();
					assertThat( customRead, is( "conv * 1" ) );
				}
		);
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final EntityMappingType entityMapping = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( ReadWriteEntity.class );
		final BasicAttributeMapping attribute = (BasicAttributeMapping) entityMapping.findAttributeMapping( "value" );
		attribute.forEachSelectable(
				(i, selectable) -> {
					final String readExpression = selectable.getCustomReadExpression();
				}
		);

		scope.inTransaction(
				(session) -> {
					session.createQuery( "from ReadWriteEntity" ).list();
				}
		);
	}

	@Test
	public void testDisambiguity(SessionFactoryScope scope) {
		// more-or-less, make sure the read-fragment has its aliases handled.
		//
		// the double reference to the entity will mean we would have an
		// ambiguous reference to the underlying `conv` column

		scope.inTransaction(
				(session) -> {
					session.createQuery( "from ReadWriteEntity a, ReadWriteEntity b", Object[].class ).list();
				}
		);
	}
}
