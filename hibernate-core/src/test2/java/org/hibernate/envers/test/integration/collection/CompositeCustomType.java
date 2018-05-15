/**
 * 
 */
package org.hibernate.envers.test.integration.collection;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.collection.CompositeCustomTypeSetEntity;
import org.hibernate.envers.test.entities.customtype.Component;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class CompositeCustomType extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CompositeCustomTypeSetEntity.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9207")
	@FailureExpected(jiraKey = "HHH-9207")
	public void testRemoval() {
		EntityManager em = getEntityManager();

		final Component comp1 = new Component( null, 11 );
		final Component comp2 = new Component( null, 22 );

		final CompositeCustomTypeSetEntity entity = new CompositeCustomTypeSetEntity();
		entity.setComponents( new HashSet<Component>() );
		entity.getComponents().add( comp1 );
		entity.getComponents().add( comp2 );

		em.getTransaction().begin();
		em.persist( entity );
		em.getTransaction().commit();

		em.getTransaction().begin();
		entity.getComponents().remove( comp1 );
		em.getTransaction().commit();

		CompositeCustomTypeSetEntity rev1 = getAuditReader().find( CompositeCustomTypeSetEntity.class, entity.getId(), 1 );
		CompositeCustomTypeSetEntity rev2 = getAuditReader().find( CompositeCustomTypeSetEntity.class, entity.getId(), 2 );
		assertEquals( "Unexpected components", TestTools.makeSet( comp1, comp2 ), rev1.getComponents() );
		assertEquals( "Unexpected components", TestTools.makeSet( comp2 ), rev2.getComponents() );

	}

}
