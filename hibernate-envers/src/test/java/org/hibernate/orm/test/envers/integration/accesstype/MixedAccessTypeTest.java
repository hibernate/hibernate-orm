/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Jpa(
		annotatedClasses = {
				MixedAccessTypeEntity.class
		}
)
@EnversTest
public class MixedAccessTypeTest {
	private Integer id1;


	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					MixedAccessTypeEntity mate = new MixedAccessTypeEntity( "data" );
					em.persist( mate );
					id1 = mate.readId();
					em.getTransaction().commit();

					em.getTransaction().begin();
					mate = em.find( MixedAccessTypeEntity.class, id1 );
					mate.writeData( "data2" );
				}
		);
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager(

				em -> {
					List<Number> revisions = AuditReaderFactory.get( em )
							.getRevisions( MixedAccessTypeEntity.class, id1 );
					assertThat( revisions ).isEqualTo( Arrays.asList( 1, 2 ) );
				} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					MixedAccessTypeEntity ver1 = new MixedAccessTypeEntity( id1, "data" );
					MixedAccessTypeEntity ver2 = new MixedAccessTypeEntity( id1, "data2" );

					MixedAccessTypeEntity rev1 = AuditReaderFactory.get( entityManager )
							.find( MixedAccessTypeEntity.class, id1, 1 );
					MixedAccessTypeEntity rev2 = AuditReaderFactory.get( entityManager )
							.find( MixedAccessTypeEntity.class, id1, 2 );

					assertThat( rev1.isDataSet() ).isTrue();
					assertThat( rev2.isDataSet() ).isTrue();

					assertThat( rev1.equals( ver1 ) ).isTrue();
					assertThat( rev2.equals( ver2 ) ).isTrue();
				}
		);

	}
}
