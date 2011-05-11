package org.hibernate.metamodel.source.annotations.xml.mocker;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.Test;

import org.hibernate.metamodel.source.annotations.JPADotNames;

import static org.junit.Assert.assertEquals;

/**
 * @author Strong Liu
 */
public class EntityListenerTest extends AbstractMockerTest {
	@Test
	public void basicEntityListenerMockTest() {
		Index index = getMockedIndex( "listener.xml" );
		index.printAnnotations();
		DotName itemName = DotName.createSimple( Item.class.getName() );
		ClassInfo itemClassInfo = index.getClassByName( itemName );
		assertEquals( 2, itemClassInfo.annotations().size() );
		assertHasAnnotation( index,itemName, JPADotNames.ENTITY );
		assertHasAnnotation( index,itemName,JPADotNames.ENTITY_LISTENERS );
	}
}
