package org.hibernate.envers.test.integration.interfaces.hbm.allAudited.subclass;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;
import org.testng.annotations.Test;

/**
 * @author Hernán Chanfreau
 *
 */
public class SubclassAllAuditedTest extends AbstractAllAuditedTest {

    public void configure(Ejb3Configuration cfg) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/interfaces/subclassAllAuditedMappings.hbm.xml");
	        cfg.addFile(new File(url.toURI()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    }

    @Test
    @Override
    public void testRetrieveAudited() {
    	super.testRetrieveAudited();
    }

    @Test
    @Override
    public void testRetrieveNonAudited() {
        super.testRetrieveNonAudited();
    }

    @Test
    @Override
    public void testRevisions() {
        super.testRevisions();
    }
    
}
