/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.idclass;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import junit.framework.Assert;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-4751")
public class IdClassWithRelationTest extends BaseEnversJPAFunctionalTestCase {
	private RelationalClassId entityId = null;
	private String typeId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {SampleClass.class, RelationalClassId.class, ClassType.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		ClassType type = new ClassType( "type", "initial description" );
		SampleClass entity = new SampleClass();
		entity.setType( type );
		entity.setSampleValue( "initial data" );
		em.persist( type );
		em.persist( entity );
		em.getTransaction().commit();

		typeId = type.getType();
		entityId = new RelationalClassId( entity.getId(), new ClassType( "type", "initial description" ) );

		// Revision 2
		em.getTransaction().begin();
		type = em.find( ClassType.class, type.getType() );
		type.setDescription( "modified description" );
		em.merge( type );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		entity = em.find( SampleClass.class, entityId );
		entity.setSampleValue( "modified data" );
		em.merge( entity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( ClassType.class, typeId ) );
		Assert.assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( SampleClass.class, entityId ) );
	}

	@Test
	public void testHistoryOfEntity() {
		// given
		SampleClass entity = new SampleClass( entityId.getId(), entityId.getType(), "initial data" );

		// when
		SampleClass ver1 = getAuditReader().find( SampleClass.class, entityId, 1 );

		// then
		Assert.assertEquals( entity.getId(), ver1.getId() );
		Assert.assertEquals( entity.getSampleValue(), ver1.getSampleValue() );
		Assert.assertEquals( entity.getType().getType(), ver1.getType().getType() );
		Assert.assertEquals( entity.getType().getDescription(), ver1.getType().getDescription() );

		// given
		entity.setSampleValue( "modified data" );
		entity.getType().setDescription( "modified description" );

		// when
		SampleClass ver2 = getAuditReader().find( SampleClass.class, entityId, 3 );

		// then
		Assert.assertEquals( entity.getId(), ver2.getId() );
		Assert.assertEquals( entity.getSampleValue(), ver2.getSampleValue() );
		Assert.assertEquals( entity.getType().getType(), ver2.getType().getType() );
		Assert.assertEquals( entity.getType().getDescription(), ver2.getType().getDescription() );
	}

	@Test
	public void testHistoryOfType() {
		// given
		ClassType type = new ClassType( typeId, "initial description" );

		// when
		ClassType ver1 = getAuditReader().find( ClassType.class, typeId, 1 );

		// then
		Assert.assertEquals( type, ver1 );
		Assert.assertEquals( type.getDescription(), ver1.getDescription() );

		// given
		type.setDescription( "modified description" );

		// when
		ClassType ver2 = getAuditReader().find( ClassType.class, typeId, 2 );

		// then
		Assert.assertEquals( type, ver2 );
		Assert.assertEquals( type.getDescription(), ver2.getDescription() );
	}
}
