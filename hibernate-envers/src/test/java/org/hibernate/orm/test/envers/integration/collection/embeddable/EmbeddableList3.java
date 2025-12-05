/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestNoProxyEntity;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableListEntity3;
import org.hibernate.orm.test.envers.entities.components.relations.ManyToOneEagerComponent;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cankut Guven
 */
@JiraKey(value = "HHH-11364")
@EnversTest
@Jpa(annotatedClasses = {EmbeddableListEntity3.class, StrTestNoProxyEntity.class})
public class EmbeddableList3 {
	private Integer ele3_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			EmbeddableListEntity3 ele3 = new EmbeddableListEntity3();
			ele3.getComponentList().add( new ManyToOneEagerComponent( null, "data" ) );
			em.persist( ele3 );
			ele3_id = ele3.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( EmbeddableListEntity3.class, ele3_id ) );
		} );
	}

	@Test
	public void testCollectionOfEmbeddableWithNullJoinColumn(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final EmbeddableListEntity3 ele3 = auditReader.find( EmbeddableListEntity3.class, ele3_id, 1 );
			assertEquals( 1, ele3.getComponentList().size(), "Expected there to be elements in the list" );
		} );
	}
}
