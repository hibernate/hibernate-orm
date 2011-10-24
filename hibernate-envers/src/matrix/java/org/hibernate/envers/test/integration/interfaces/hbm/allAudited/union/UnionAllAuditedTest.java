package org.hibernate.envers.test.integration.interfaces.hbm.allAudited.union;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class UnionAllAuditedTest extends AbstractAllAuditedTest {

    public void configure(Ejb3Configuration cfg) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/interfaces/unionAllAuditedMappings.hbm.xml");
	        cfg.addFile(new File(url.toURI()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    }
}
