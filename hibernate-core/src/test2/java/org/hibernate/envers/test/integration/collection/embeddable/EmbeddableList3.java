/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestNoProxyEntity;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity3;
import org.hibernate.envers.test.entities.components.relations.ManyToOneEagerComponent;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Cankut Guven
 */
@TestForIssue(jiraKey = "HHH-11364")
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
