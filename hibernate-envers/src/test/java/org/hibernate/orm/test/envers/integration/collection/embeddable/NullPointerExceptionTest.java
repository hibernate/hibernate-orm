/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11215")
@EnversTest(auditStrategies = ValidityAuditStrategy.class)
@Jpa(annotatedClasses = {Product.class, Type.class})
public class NullPointerExceptionTest {
	private Integer productId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		this.productId = scope.fromTransaction( entityManager -> {
			Product product = new Product( 1 , "Test" );
			product.getItems().add( new Item( "bread", null ) );
			entityManager.persist( product );
			return product.getId();
		} );

		// Revision 2
		scope.inTransaction( entityManager -> {
			Type type = new Type( 2, "T2" );
			entityManager.persist( type );
			Product product = entityManager.find( Product.class, productId );
			product.getItems().add( new Item( "bread2", type ) );
			entityManager.merge( product );
		} );

		// Revision 3
		scope.inTransaction( entityManager -> {
			Product product = entityManager.find( Product.class, productId );
			product.getItems().remove( 0 );
			entityManager.merge( product );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( Product.class, productId ) );
			assertEquals( 1, auditReader.find( Product.class, productId, 1 ).getItems().size() );
			assertEquals( 2, auditReader.find( Product.class, productId, 2 ).getItems().size() );
			assertEquals( 1, auditReader.find( Product.class, productId, 3 ).getItems().size() );
		} );
	}

	@Test
	public void testRevision1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final Product product = auditReader.find( Product.class, productId, 1 );
			assertEquals( 1, product.getItems().size() );
			assertEquals( "bread", product.getItems().get( 0 ).getName() );
		} );
	}

	@Test
	public void testRevision2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final Product product = auditReader.find( Product.class, productId, 2 );
			assertEquals( 2, product.getItems().size() );
			assertEquals( "bread", product.getItems().get( 0 ).getName() );
			assertEquals( "bread2", product.getItems().get( 1 ).getName() );
			assertEquals( new Type( 2, "T2" ), product.getItems().get( 1 ).getType() );
		} );
	}

	@Test
	public void testRevision3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final Product product = auditReader.find( Product.class, productId, 3 );
			assertEquals( 1, product.getItems().size() );
			assertEquals( "bread2", product.getItems().get( 0 ).getName() );
			assertEquals( new Type( 2, "T2" ), product.getItems().get( 0 ).getType() );
		} );
	}
}
