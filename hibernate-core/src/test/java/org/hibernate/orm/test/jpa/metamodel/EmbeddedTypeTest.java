/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import java.sql.Date;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = { Product.class, ShelfLife.class, VersionedEntity.class }
)
public class EmbeddedTypeTest {

	@Test
	@JiraKey(value = "HHH-6896")
	public void ensureComponentsReturnedAsManagedType(EntityManagerFactoryScope scope) {
		ManagedType<ShelfLife> managedType = scope.getEntityManagerFactory()
				.getMetamodel()
				.managedType( ShelfLife.class );
		// the issue was in regards to throwing an exception, but also check for nullness
		assertNotNull( managedType );
	}

	@Test
	@JiraKey(value = "HHH-4702")
	public void testSingularAttributeAccessByName(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					SingularAttribute soldDate_ = entityManager.getMetamodel().embeddable( ShelfLife.class )
							.getSingularAttribute( "soldDate" );
					assertEquals( Date.class, soldDate_.getJavaType() );
					assertEquals( Date.class, soldDate_.getType().getJavaType() );
					assertEquals( Date.class, soldDate_.getJavaType() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5821")
	public void testVersionAttributeMetadata(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					EntityType<VersionedEntity> metadata = entityManager.getMetamodel().entity( VersionedEntity.class );
					assertNotNull( metadata.getDeclaredVersion( int.class ) );
					assertTrue( metadata.getDeclaredVersion( int.class ).isVersion() );
					assertEquals( 3, metadata.getDeclaredSingularAttributes().size() );
					assertTrue( metadata.getDeclaredSingularAttributes()
										.contains( metadata.getDeclaredVersion( int.class ) ) );
				}
		);
	}

}
