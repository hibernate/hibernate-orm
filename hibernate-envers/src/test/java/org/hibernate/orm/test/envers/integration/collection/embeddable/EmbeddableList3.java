/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestNoProxyEntity;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableListEntity3;
import org.hibernate.orm.test.envers.entities.components.relations.ManyToOneEagerComponent;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Cankut Guven
 */
@JiraKey(value = "HHH-11364")
public class EmbeddableList3 extends BaseEnversJPAFunctionalTestCase {
	private Integer ele3_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EmbeddableListEntity3.class, StrTestNoProxyEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		ele3_id = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			EmbeddableListEntity3 ele3 = new EmbeddableListEntity3();
			ele3.getComponentList().add( new ManyToOneEagerComponent( null, "data" ) );
			entityManager.persist( ele3 );
			return ele3.getId();
		} );
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( EmbeddableListEntity3.class, ele3_id) );
	}

	@Test
	public void testCollectionOfEmbeddableWithNullJoinColumn() {
		final EmbeddableListEntity3 ele3 = getAuditReader().find( EmbeddableListEntity3.class, ele3_id, 1 );
		assertEquals( "Expected there to be elements in the list", 1, ele3.getComponentList().size() );
	}
}
