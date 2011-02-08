//$Id$
package org.hibernate.test.annotations.reflection;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.dom4j.io.SAXReader;
import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.hibernate.util.XMLHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotSupportedException;

/**
 * @author Emmanuel Bernard
 */
public class XMLContextTest extends TestCase {
	public void testAll() throws Exception {
		XMLHelper xmlHelper = new XMLHelper();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream(
				"org/hibernate/test/annotations/reflection/orm.xml"
		);
		assertNotNull( "ORM.xml not found", is );
		XMLContext context = new XMLContext();
		List errors = new ArrayList();
		SAXReader saxReader = xmlHelper.createSAXReader( "XML InputStream", errors, EJB3DTDEntityResolver.INSTANCE );
		//saxReader.setValidation( false );
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
		}
		catch (SAXNotSupportedException e) {
			saxReader.setValidation( false );
		}
		org.dom4j.Document doc;
		try {
			doc = saxReader
					.read( new InputSource( new BufferedInputStream( is ) ) );
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ioe) {
				//log.warn( "Could not close input stream", ioe );
			}
		}
		assertEquals( 0, errors.size() );
		context.addDocument( doc );
	}
}
