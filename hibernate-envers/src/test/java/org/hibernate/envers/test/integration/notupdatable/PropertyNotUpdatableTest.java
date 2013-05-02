package org.hibernate.envers.test.integration.notupdatable;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;
import junit.framework.Assert;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-5411")
public class PropertyNotUpdatableTest extends BaseEnversJPAFunctionalTestCase {
	private Long id = null;

	@Override
	protected void addConfigOptions(Map options) {
		options.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {PropertyNotUpdatableEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		PropertyNotUpdatableEntity entity = new PropertyNotUpdatableEntity(
				"data",
				"constant data 1",
				"constant data 2"
		);
		em.persist( entity );
		em.getTransaction().commit();

		id = entity.getId();

		// Revision 2
		em.getTransaction().begin();
		entity = em.find( PropertyNotUpdatableEntity.class, entity.getId() );
		entity.setData( "modified data" );
		entity.setConstantData1( null );
		em.merge( entity );
		em.getTransaction().commit();

		em.close();
		em = getEntityManager(); // Re-opening entity manager to re-initialize non-updatable fields
		// with database values. Otherwise PostUpdateEvent#getOldState() returns previous
		// memory state. This can be achieved using EntityManager#refresh(Object) method as well.

		// Revision 3
		em.getTransaction().begin();
		entity = em.find( PropertyNotUpdatableEntity.class, entity.getId() );
		entity.setData( "another modified data" );
		entity.setConstantData2( "invalid data" );
		em.merge( entity );
		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();
		em.refresh( entity );
		em.remove( entity );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals(
				Arrays.asList( 1, 2, 3, 4 ),
				getAuditReader().getRevisions( PropertyNotUpdatableEntity.class, id )
		);
	}

	@Test
	public void testHistoryOfId() {
		PropertyNotUpdatableEntity ver1 = new PropertyNotUpdatableEntity(
				"data",
				"constant data 1",
				"constant data 2",
				id
		);
		Assert.assertEquals( ver1, getAuditReader().find( PropertyNotUpdatableEntity.class, id, 1 ) );

		PropertyNotUpdatableEntity ver2 = new PropertyNotUpdatableEntity(
				"modified data",
				"constant data 1",
				"constant data 2",
				id
		);
		Assert.assertEquals( ver2, getAuditReader().find( PropertyNotUpdatableEntity.class, id, 2 ) );

		PropertyNotUpdatableEntity ver3 = new PropertyNotUpdatableEntity(
				"another modified data",
				"constant data 1",
				"constant data 2",
				id
		);
		Assert.assertEquals( ver3, getAuditReader().find( PropertyNotUpdatableEntity.class, id, 3 ) );
	}

	@Test
	public void testDeleteState() {
		PropertyNotUpdatableEntity delete = new PropertyNotUpdatableEntity(
				"another modified data",
				"constant data 1",
				"constant data 2",
				id
		);
		List<Object> results = getAuditReader().createQuery().forRevisionsOfEntity(
				PropertyNotUpdatableEntity.class,
				true,
				true
		).getResultList();
		Assert.assertEquals( delete, results.get( 3 ) );
	}
}
