/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.AttributeConverter;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.LIBRARY)
@SessionFactory
public class Jpa4ParameterBindingTests {

	@Test
	void testHql(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book where id = :id", Book.class )
					.setConvertedParameter( "id", "123", StringToIntConverter.class )
					.list();

			session.createQuery( "from Book where id = ?1", Book.class )
					.setConvertedParameter( 1, "123", StringToIntConverter.class )
					.list();

			session.createQuery( "from Book where id = :id", Book.class )
					.setParameter( "id", 1, Integer.class )
					.list();

			session.createQuery( "from Book where id = ?1", Book.class )
					.setParameter( 1, 1, Integer.class )
					.list();
		} );
	}

	@Test
	void testNativeQuery(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createNativeQuery( "select * from books where id = :id", Book.class )
					.setConvertedParameter( "id", "123", StringToIntConverter.class )
					.list();

			session.createNativeQuery( "select * from books where id = ?1", Book.class )
					.setConvertedParameter( 1, "123", StringToIntConverter.class )
					.list();

			session.createNativeQuery( "select * from books where id = :id", Book.class )
					.setParameter( "id", 1, Integer.class )
					.list();

			session.createNativeQuery( "select * from books where id = ?1", Book.class )
					.setParameter( 1, 1, Integer.class )
					.list();
		} );

	}

	public static class StringToIntConverter implements AttributeConverter<String,Integer> {
		@Override
		public Integer convertToDatabaseColumn(String domainValue) {
			return domainValue == null ? null : Integer.parseInt( domainValue );
		}

		@Override
		public String convertToEntityAttribute(Integer relationalValue) {
			return relationalValue == null ? null : Integer.toString( relationalValue );
		}
	}
}
