package org.hibernate.jpa.test.multimappingperclass;

import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-8775")
public class MappingClassMoreThanOnceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/jpa/test/multimappingperclass/ClassMappedMoreThanOnce.hbm.xml" };
	}

	/**
	 * Tests that an entity manager can be created when a class is mapped more than once.
	 */
	@Test
	@FailureExpected(jiraKey = "HHH-8775")
	public void testEntityManagerCreationWithEntityNameMapping() {
		EntityManager entityManager = getOrCreateEntityManager();
		assertNotNull( "Expected an entity manager to be returned", entityManager );
	}

}
