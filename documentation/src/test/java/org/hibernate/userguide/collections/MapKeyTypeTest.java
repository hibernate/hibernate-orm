/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MapKeyTypeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			PersonDummy.class,
		};
	}

	@Test
	public void testLifecycle() {

		LocalDateTime firstCall = LocalDateTime.of(
			2017, 5, 23, 18, 21, 57
		);

		LocalDateTime secondCall = LocalDateTime.of(
			2017, 5, 23, 18, 22, 19
		);

		doInJPA( this::entityManagerFactory, entityManager -> {
			PersonDummy person = new PersonDummy();
			person.setId( 1L );
			person.getPhoneRegister().put( Timestamp.valueOf( firstCall ).getTime(), 101 );
			person.getPhoneRegister().put( Timestamp.valueOf( secondCall ).getTime(), 102 );
			entityManager.persist( person );
		} );

		EntityManagerFactory entityManagerFactory = null;
		try {
			Map settings = buildSettings();
			settings.put(
				org.hibernate.jpa.AvailableSettings.LOADED_CLASSES,
				Collections.singletonList(
					Person.class
				)
			);
			settings.put(
					AvailableSettings.HBM2DDL_AUTO,
					"none"
			);
			entityManagerFactory =  Bootstrap.getEntityManagerFactoryBuilder(
					new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
					settings
			).build().unwrap( SessionFactoryImplementor.class );

			final EntityManagerFactory emf = entityManagerFactory;

			doInJPA( () -> emf, entityManager -> {
				Person person = entityManager.find( Person.class, 1L );
				assertEquals(
					Integer.valueOf( 101 ),
					person.getCallRegister().get( Timestamp.valueOf( firstCall ) )
				);
			} );
		}
		finally {
			if ( entityManagerFactory != null ) {
				entityManagerFactory.close();
			}
		}
	}

	@Entity
	@Table(name = "person")
	public static class PersonDummy {

		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(
			name = "call_register",
			joinColumns = @JoinColumn(name = "person_id")
		)
		@MapKeyColumn( name = "call_timestamp_epoch" )
		@Column(name = "phone_number")
		private Map<Long, Integer> callRegister = new HashMap<>();

		public void setId(Long id) {
			this.id = id;
		}

		public Map<Long, Integer> getPhoneRegister() {
			return callRegister;
		}
	}

	//tag::collections-map-custom-key-type-mapping-example[]
	@Entity
	@Table(name = "person")
	public static class Person {

		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(
			name = "call_register",
			joinColumns = @JoinColumn(name = "person_id")
		)
		@MapKeyType(
			@Type(
				type = "org.hibernate.userguide.collections.type.TimestampEpochType"
			)
		)
		@MapKeyColumn( name = "call_timestamp_epoch" )
		@Column(name = "phone_number")
		private Map<Date, Integer> callRegister = new HashMap<>();

		//Getters and setters are omitted for brevity

	//end::collections-map-custom-key-type-mapping-example[]

		public void setId(Long id) {
			this.id = id;
		}

		public Map<Date, Integer> getCallRegister() {
			return callRegister;
		}
	//tag::collections-map-custom-key-type-mapping-example[]
	}
	//end::collections-map-custom-key-type-mapping-example[]
}
