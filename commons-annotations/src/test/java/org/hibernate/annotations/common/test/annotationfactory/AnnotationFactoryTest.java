package org.hibernate.annotations.common.test.annotationfactory;

import junit.framework.TestCase;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;

/**
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
public class AnnotationFactoryTest extends TestCase {

	public void testCreatesProxyInstancesOfAnnotations() {
		// Step 1: define the annotation descriptor.
		//
		// What you can/must do:
		// - You can assign values with the wrong type to the annotation
		//   elements. The code won't check that the values are the same
		//   types as required by the Annotation interface. You will
		//   end up receiving an exception when you access the value, though.
		// - You must assign a value in the descriptor to all the elements
		//   defined in the Annotation interface that do not have a default
		//   value.
		// - You can ignore in the descriptor those Annotation elements that
		//   have default values, or you can set them to override their
		//   default values.
		AnnotationDescriptor descriptor = new AnnotationDescriptor( TestAnnotation.class );
		descriptor.setValue( "booleanElement", false );
		descriptor.setValue( "stringElement", "abc" );
		descriptor.setValue( "someOtherElement", "xyz" );

		// Step 2: create the annotation from its descriptor.
		TestAnnotation ann = AnnotationFactory.create( descriptor );

		assertFalse( ann.booleanElement() );
		assertEquals( "abc", ann.stringElement() );
	}
}
