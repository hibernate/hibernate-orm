package org.hibernate.jpamodelgen.test.usertype.jpa;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

public class JpaUserTypeTest extends CompilationTest {

	@Test
	@WithClasses({
			ConcreteSimpleType.class,
			ConcreteSerializableSimpleType.class,
			EntityWithSimpleTypes.class,
			AbstractSerializableSimpleType.class,
			AbstractSimpleType.class
	})
	public void testCustomUserTypeInMetaModel() {
		
		assertMetamodelClassGeneratedFor( EntityWithSimpleTypes.class );
		
		assertPresenceOfFieldInMetamodelFor(
				EntityWithSimpleTypes.class, "integerValue"
		);
		
		assertPresenceOfFieldInMetamodelFor(
				EntityWithSimpleTypes.class, "concreteSerializableSimpleType"
		);
		
		assertPresenceOfFieldInMetamodelFor(
				EntityWithSimpleTypes.class, "concreteSerializableSimpleTypeImplementingSerializable"
		);
		
	}
	
}
