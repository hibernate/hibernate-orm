/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.properties;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Nicolas Doroskevich
 */
@EnversTest
@DomainModel(annotatedClasses = {UnversionedOptimisticLockingFieldEntity.class})
@ServiceRegistry(settings = @Setting(name = EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD, value = "true"))
@SessionFactory
public class UnversionedOptimisticLockingField {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			UnversionedOptimisticLockingFieldEntity olfe = new UnversionedOptimisticLockingFieldEntity( "x" );
			em.persist( olfe );
			id1 = olfe.getId();
		} );

		scope.inTransaction( em -> {
			UnversionedOptimisticLockingFieldEntity olfe = em.find( UnversionedOptimisticLockingFieldEntity.class,
					id1 );
			olfe.setStr( "y" );
		} );
	}

	@Test
	public void testRevisionCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( UnversionedOptimisticLockingFieldEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			UnversionedOptimisticLockingFieldEntity ver1 = new UnversionedOptimisticLockingFieldEntity( id1, "x" );
			UnversionedOptimisticLockingFieldEntity ver2 = new UnversionedOptimisticLockingFieldEntity( id1, "y" );

			assertEquals( ver1, auditReader.find( UnversionedOptimisticLockingFieldEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( UnversionedOptimisticLockingFieldEntity.class, id1, 2 ) );
		} );
	}

	@Test
	public void testMapping(DomainModelScope scope) {
		PersistentClass pc = scope.getDomainModel()
				.getEntityBinding( UnversionedOptimisticLockingFieldEntity.class.getName() + "_AUD" );
		for ( Property p : pc.getProperties() ) {
			assertNotEquals( "optLocking", p.getName() );
		}
	}
}
