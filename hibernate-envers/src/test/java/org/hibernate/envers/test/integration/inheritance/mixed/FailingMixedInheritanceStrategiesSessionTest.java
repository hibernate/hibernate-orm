package org.hibernate.envers.test.integration.inheritance.mixed;

import org.hibernate.MappingException;
import org.hibernate.testing.FailureExpected;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
public class FailingMixedInheritanceStrategiesSessionTest extends AbstractInheritanceStrategiesSessionTest {

    protected void initMappings() throws MappingException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(
                "mappings/mixedInheritanceStrategies/mappingsFailing.hbm.xml");
        config.addFile(new File(url.toURI()));
    }

    @Test
    @FailureExpected(message = "Problem with mixed inheritance strategies", jiraKey = "HHH-6177")
    public void testFirstRevisionOfCheckInActivity() throws Exception {
        doTestFirstRevisionOfCheckInActivity();
    }

    @Test
    @FailureExpected(message = "Problem with mixed inheritance strategies", jiraKey = "HHH-6177")
    public void testSecondRevisionOfCheckInActivity() throws Exception {
        doTestSecondRevisionOfCheckInActivity();
    }
}
