package org.hibernate.procedure.internal;

import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nathan Xu
 */
public class ProcedureCallImplTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-13644" )
	@RequiresDialect( H2Dialect.class )
	public void testNoNullPointerExceptionThrown() {

		EntityManager em = getOrCreateEntityManager();

		em.getTransaction().begin();

		em.createNativeQuery("CREATE ALIAS GET_RANDOM_VALUE FOR \"java.lang.Math.random\";").executeUpdate();

		Query query = em.createStoredProcedureQuery("GET_RANDOM_VALUE");

		Stream stream = query.getResultStream();

		Assert.assertEquals(1, stream.count());

		em.getTransaction().commit();

		em.close();
	}
}
