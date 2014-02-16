package org.hibernate.envers.test.integration.components;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Felix Feisst
 */
@TestForIssue(jiraKey = "HHH-8968")
public class ComponentWithNullValueTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CollectionOfComponentTestEntity.class };
	}

	@Test
	public void testComponentWithNullValue() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		final CollectionOfComponentTestEntity entity = new CollectionOfComponentTestEntity();
		final Set<Component1> set = new HashSet<Component1>();
		Component1 component = new Component1( "string1", null );
		set.add( component );
		entity.setComp1s( set );
		em.persist( entity );
		em.getTransaction().commit();
		em.close();

		em = getEntityManager();
		CollectionOfComponentTestEntity reloaded = em.find( CollectionOfComponentTestEntity.class, entity.getId() );
		assertEquals( "Expected a component", entity.getComp1s(), reloaded.getComp1s() );

		CollectionOfComponentTestEntity entityV1 = getAuditReader().find( CollectionOfComponentTestEntity.class, entity.getId(), 1 );
		assertEquals( "Expected a component", Collections.singleton( component ), entityV1.getComp1s() );
	}

}
