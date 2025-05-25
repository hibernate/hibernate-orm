/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				JpaProxyComplianceEnabledTest.Provider.class,
				JpaProxyComplianceEnabledTest.TelephoneNumber.class,
		},
		proxyComplianceEnabled = true
)
@JiraKey("HHH-19476")
public class JpaProxyComplianceEnabledTest {

	private static final Integer PROVIDER_ID = 1;
	private static final Integer TELEPHONE_NUMBER_ID = 2;

	@BeforeAll
	public static void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Provider provider = new Provider( PROVIDER_ID, "A Provider" );
			entityManager.persist( provider );

			TelephoneNumber telephoneNumber1 = new TelephoneNumber(
					TELEPHONE_NUMBER_ID,
					"123-456-7890",
					provider
			);
			entityManager.persist( telephoneNumber1 );
		} );
	}

	@Test
	public void testJoinFetchAfterFind(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					TelephoneNumber telephoneNumber = entityManager.find( TelephoneNumber.class, TELEPHONE_NUMBER_ID );
					List<TelephoneNumber> telephoneNumbers = entityManager.createQuery(
									"from TelephoneNumber t join fetch t.provider",
									TelephoneNumber.class )
							.getResultList();
					assertThat( telephoneNumbers.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "TelephoneNumber")
	public static class TelephoneNumber {
		@Id
		private Integer id;

		@Column(name = "phone_number")
		private String number;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "provider", nullable = false)
		private Provider provider;

		public TelephoneNumber() {
		}

		public TelephoneNumber(Integer id, String number, Provider provider) {
			this.id = id;
			this.number = number;
			this.provider = provider;
		}

		public Integer getId() {
			return id;
		}

		public String getNumber() {
			return number;
		}

		public Provider getProvider() {
			return provider;
		}
	}

	@Entity(name = "Provider")
	public static class Provider {

		@Id
		private Integer id;

		private String name;

		public Provider() {
		}

		public Provider(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}

}
