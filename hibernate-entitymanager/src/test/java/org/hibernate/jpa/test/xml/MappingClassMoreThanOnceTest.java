package org.hibernate.jpa.test.xml;

import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-8775")
public class MappingClassMoreThanOnceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/jpa/test/xml/ClassMappedMoreThanOnce.hbm.xml" };
	}

	@Test
	@FailureExpected(jiraKey = "HHH-8775")
	public void testEntityManagerCreationWithEntityNameMapping() {
		EntityManager entityManager = getOrCreateEntityManager();
		assertNotNull( "Expected an entity manager to be returned", entityManager );
	}

}
