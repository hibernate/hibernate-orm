package org.hibernate.metamodel.source.annotations.xml.mocker;

import org.jboss.jandex.Index;
import org.junit.Test;

/**
 * @author Strong Liu
 */
public class PersistenceMetadataMockerTest extends AbstractMockerTest {
	@Test
	public void testPersistenceMetadata() {
		Index index = getMockedIndex( "persistence-metadata.xml" );
		index.printAnnotations();
	}
}
