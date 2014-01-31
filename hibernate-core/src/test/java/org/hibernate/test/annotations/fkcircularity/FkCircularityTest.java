package org.hibernate.test.annotations.fkcircularity;

import org.hibernate.metamodel.MetadataSources;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * Test case for ANN-722 and ANN-730.
 *
 * @author Hardy Ferentschik
 */
public class FkCircularityTest extends BaseUnitTestCase {
    @Test
	@FailureExpectedWithNewMetamodel
	public void testJoinedSublcassesInPK() {
		// metamodel : fails with error about D#id annotated with @Embedded but D_PK not annotated as @Embeddable
		// however, adding @Embeddable to D_PK makes no difference; same error...

		MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( B.class )
				.addAnnotatedClass( C.class )
				.addAnnotatedClass( D.class );
		metadataSources.buildMetadata();
	}
}
