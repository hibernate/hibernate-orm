package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.union;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.AbstractPropertiesAudited2Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Hern�n Chanfreau
 *
 */
public class UnionPropertiesAudited2Test extends AbstractPropertiesAudited2Test {

    public void configure(Ejb3Configuration cfg) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/interfaces/unionPropertiesAudited2Mappings.hbm.xml");
	        cfg.addFile(new File(url.toURI()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    }
}
