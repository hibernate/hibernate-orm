package org.hibernate.processor.test.data.namecollision;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class LifecycleNameCollisionTest {
	@Test
	@WithClasses({ Event.class, LifecycleEvent.class, Inject.class })
	void generatesCompilableListenersForConflictingTypeNames() {
		assertTrue( getMetaModelSourceAsString( Event.class, true ).contains( "jakarta.enterprise.event.Event<" ) );
		assertTrue( getMetaModelSourceAsString( LifecycleEvent.class, true ).contains( "jakarta.data.event.LifecycleEvent<" ) );
		assertTrue( getMetaModelSourceAsString( Inject.class, true ).contains( "@jakarta.inject.Inject" ) );
	}
}
