package org.hibernate.envers.test.integration.superclass.auditparents;

import javax.persistence.EntityManager;
import javax.persistence.MappedSuperclass;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests mapping of child entity that declares one of its ancestors as audited with {@link Audited#auditParents()} property.
 * All supperclasses are marked with {@link MappedSuperclass} annotation but not {@link Audited}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SingleAuditParentsTest extends BaseEnversJPAFunctionalTestCase {
	private long childSingleId = 1L;
	private Integer siteSingleId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MappedGrandparentEntity.class,
				MappedParentEntity.class,
				ChildSingleParentEntity.class,
				StrIntTestEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		// Revision 1
		em.getTransaction().begin();
		StrIntTestEntity siteSingle = new StrIntTestEntity( "data 1", 1 );
		em.persist( siteSingle );
		em.persist(
				new ChildSingleParentEntity(
						childSingleId,
						"grandparent 1",
						"notAudited 1",
						"parent 1",
						"child 1",
						siteSingle
				)
		);
		em.getTransaction().commit();
		siteSingleId = siteSingle.getId();
		em.close();
	}

	@Test
	public void testCreatedAuditTable() {
		Set<String> expectedColumns = TestTools.makeSet( "child", "grandparent", "id" );
		Set<String> unexpectedColumns = TestTools.makeSet( "parent", "relation_id", "notAudited" );

		Table table = getCfg().getClassMapping(
				"org.hibernate.envers.test.integration.superclass.auditparents.ChildSingleParentEntity_AUD"
		).getTable();

		for ( String columnName : expectedColumns ) {
			// Check whether expected column exists.
			Assert.assertNotNull( table.getColumn( new Column( columnName ) ) );
		}
		for ( String columnName : unexpectedColumns ) {
			// Check whether unexpected column does not exist.
			Assert.assertNull( table.getColumn( new Column( columnName ) ) );
		}
	}

	@Test
	public void testSingleAuditParent() {
		// expectedSingleChild.parent, expectedSingleChild.relation and expectedSingleChild.notAudited shall be null, because they are not audited.
		ChildSingleParentEntity expectedSingleChild = new ChildSingleParentEntity(
				childSingleId,
				"grandparent 1",
				null,
				null,
				"child 1",
				null
		);
		ChildSingleParentEntity child = getAuditReader().find( ChildSingleParentEntity.class, childSingleId, 1 );
		Assert.assertEquals( expectedSingleChild, child );
		Assert.assertNull( child.getRelation() );
	}
}
