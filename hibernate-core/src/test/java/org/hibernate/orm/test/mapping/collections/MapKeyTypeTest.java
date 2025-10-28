/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MapKeyTypeTest extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PersonDummy.class };
	}

	@Test
	public void testLifecycle() {

		LocalDateTime firstCall = LocalDateTime.of(
			2017, 5, 23, 18, 21, 57
		);

		LocalDateTime secondCall = LocalDateTime.of(
			2017, 5, 23, 18, 22, 19
		);

		doInJPA(this::entityManagerFactory, entityManager -> {
			PersonDummy person = new PersonDummy();
			person.setId(1L);
			person.getPhoneRegister().put(Timestamp.valueOf(firstCall).getTime(), 101);
			person.getPhoneRegister().put(Timestamp.valueOf(secondCall).getTime(), 102);
			entityManager.persist(person);
		});

		EntityManagerFactory entityManagerFactory = null;
		try {
			Map settings = buildSettings();
			settings.put(
				AvailableSettings.LOADED_CLASSES,
				Collections.singletonList(
					Person.class
				)
			);
			settings.put(
					AvailableSettings.HBM2DDL_AUTO,
					"none"
			);
			entityManagerFactory =  Bootstrap.getEntityManagerFactoryBuilder(
					new TestingPersistenceUnitDescriptorImpl(getClass().getSimpleName()),
					settings
			).build().unwrap(SessionFactoryImplementor.class);

			final EntityManagerFactory emf = entityManagerFactory;

			doInJPA(() -> emf, entityManager -> {
				Person person = entityManager.find(Person.class, 1L);
				assertEquals(
					Integer.valueOf(101),
					person.getCallRegister().get(Timestamp.valueOf(firstCall))
				);
			});
		}
		finally {
			if (entityManagerFactory != null) {
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
		@MapKeyColumn(name = "call_timestamp_epoch")
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
		@MapKeyJdbcTypeCode(Types.BIGINT)
	//end::collections-map-custom-key-type-mapping-example[]
// todo (6.0) : figure out why `@MapKeyTemporal` did not work.  imo it should
//		@MapKeyTemporal(TemporalType.TIMESTAMP)
	//tag::collections-map-custom-key-type-mapping-example[]
		@MapKeyJavaType(JdbcTimestampJavaType.class)
		@MapKeyColumn(name = "call_timestamp_epoch")
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
