/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.lazy;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionEntity;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionRefEntity1;
import org.hibernate.orm.test.envers.entities.collection.MultipleCollectionRefEntity2;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Fabricio Gregorio
 */
@JiraKey(value = "HHH-15522")
@BytecodeEnhanced
@EnversTest
@Jpa(annotatedClasses = {
		MultipleCollectionEntity.class,
		MultipleCollectionRefEntity1.class,
		MultipleCollectionRefEntity2.class
})
@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle does not support identity key generation")
@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase does not support identity key generation")
public class IsCollectionInitializedBytecodeEnhancementTest {
	private Long mce1Id = null;
	private Long mcre1Id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1 - addition.
			em.getTransaction().begin();
			MultipleCollectionEntity mce1 = new MultipleCollectionEntity();
			mce1.setText( "MultipleCollectionEntity-1-1" );
			em.persist( mce1 ); // Persisting entity with empty collections.
			em.getTransaction().commit();

			mce1Id = mce1.getId();

			// Revision 2 - update.
			em.getTransaction().begin();
			mce1 = em.find( MultipleCollectionEntity.class, mce1.getId() );
			MultipleCollectionRefEntity1 mcre1 = new MultipleCollectionRefEntity1();
			mcre1.setText( "MultipleCollectionRefEntity1-1-1" );
			mcre1.setMultipleCollectionEntity( mce1 );
			mce1.addRefEntity1( mcre1 );
			em.persist( mcre1 );
			mce1 = em.merge( mce1 );
			em.getTransaction().commit();

			mcre1Id = mcre1.getId();
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testIsInitialized(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var reader = AuditReaderFactory.get( em );
			List<MultipleCollectionEntity> res = reader.createQuery()
					.forEntitiesAtRevision( MultipleCollectionEntity.class, 1 )
					.add( AuditEntity.id().eq( mce1Id ) )
					.getResultList();

			MultipleCollectionEntity ret = res.get( 0 );

			assertEquals( false, Hibernate.isInitialized( ret.getRefEntities1() ) );

			Hibernate.initialize( ret.getRefEntities1() );

			assertEquals( true, Hibernate.isInitialized( ret.getRefEntities1() ) );
		} );
	}
}
