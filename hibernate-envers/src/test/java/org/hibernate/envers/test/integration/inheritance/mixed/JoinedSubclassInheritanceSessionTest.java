package org.hibernate.envers.test.integration.inheritance.mixed;

import org.hibernate.MappingException;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
public class JoinedSubclassInheritanceSessionTest extends AbstractInheritanceStrategiesSessionTest {

    protected void initMappings() throws MappingException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(
                "mappings/mixedInheritanceStrategies/mappingsPassing.hbm.xml");
        config.addFile(new File(url.toURI()));
    }

    @Test
    public void testFirstRevisionOfCheckInActivity() throws Exception {
        doTestFirstRevisionOfCheckInActivity();
    }

    @Test
    public void testSecondRevisionOfCheckInActivity() throws Exception {
        doTestSecondRevisionOfCheckInActivity();
    }
}
