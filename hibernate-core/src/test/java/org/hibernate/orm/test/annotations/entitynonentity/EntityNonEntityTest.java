/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entitynonentity;

import org.hibernate.UnknownEntityTypeException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Phone.class,
				Voice.class,
				// Adding Cellular here is a test for HHH-9855
				Cellular.class,
				GSM.class
		}
)
@SessionFactory
public class EntityNonEntityTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						scope.getSessionFactory().getSchemaManager().truncateMappedObjects()
		);
	}

	@Test
	public void testMix(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					GSM gsm = new GSM();
					gsm.brand = "Sony";
					gsm.frequency = 900;
					gsm.isNumeric = true;
					gsm.number = 2;
					gsm.species = "human";
					session.persist( gsm );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					gsm = session.get( GSM.class, gsm.id );

					assertThat( gsm.number )
							.describedAs( "top mapped superclass" )
							.isEqualTo( 2 );
					assertThat( gsm.species )
							.describedAs( "non entity between mapped superclass and entity" )
							.isNull();
					assertThat( gsm.isNumeric )
							.describedAs( "mapped superclass under entity" )
							.isTrue();
					assertThat( gsm.brand )
							.describedAs( "non entity under entity" )
							.isNull();
					assertThat( gsm.frequency )
							.describedAs( "leaf entity" )
							.isEqualTo( 900 );

					session.remove( gsm );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9856")
	public void testGetAndFindNonEntityThrowsIllegalArgumentException(SessionFactoryScope scope) {
		assertThrows( UnknownEntityTypeException.class, () -> {
			scope.getSessionFactory().getMappingMetamodel().findEntityDescriptor( Cellular.class );
			scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Cellular.class );
		} );

		assertThrows( UnknownEntityTypeException.class, () ->
				scope.getSessionFactory().getMappingMetamodel().getMappingMetamodel()
						.getEntityDescriptor( Cellular.class.getName() )
		);

		assertThrows( UnknownEntityTypeException.class, () -> scope.inTransaction(
				session ->
						session.get( Cellular.class, 1 )
		) );

		assertThrows( UnknownEntityTypeException.class, () -> scope.inTransaction(
				session ->
						session.get( Cellular.class.getName(), 1 )
		) );
	}
}
