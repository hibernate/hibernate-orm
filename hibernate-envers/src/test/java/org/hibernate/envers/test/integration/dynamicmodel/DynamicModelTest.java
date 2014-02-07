package org.hibernate.envers.test.integration.dynamicmodel;

import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-8769")
public class DynamicModelTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "mappings/dynamicmodel/dynamicModel.hbm.xml" };
	}

	@Test
	@FailureExpected(jiraKey = "HHH-8769")
	public void testDynamicModelMapping() {
		EntityManager entityManager = getOrCreateEntityManager();
		assertNotNull( "Expected an entity manager to be returned", entityManager );
	}

}
