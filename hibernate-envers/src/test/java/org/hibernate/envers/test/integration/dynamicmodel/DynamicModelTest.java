package org.hibernate.envers.test.integration.dynamicmodel;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.EntityManager;

import static org.junit.Assert.assertNotNull;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-8769")
public class DynamicModelTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "mappings/dynamicmodel/dynamicModel.hbm.xml" };
	}

	/**
	 * Tests that an EntityManager can be created when using a dynamic model mapping.
	 */
	@Test
	public void testDynamicModelMapping() {
		EntityManager entityManager = getOrCreateEntityManager();
		assertNotNull( "Expected an entity manager to be returned", entityManager );
	}

}
