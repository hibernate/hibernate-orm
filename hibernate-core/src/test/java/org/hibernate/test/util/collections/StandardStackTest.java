package org.hibernate.test.util.collections;

import org.hibernate.internal.util.collections.StandardStack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StandardStackTest {

	@Test
	public void testGetPrevious() {

		StandardStack<String> stack = new StandardStack<>();

		stack.push( "previous" );

		assertEquals( "previous", stack.getCurrent() );
		assertNull( stack.getPrevious() );

		stack.push( "current" );

		assertEquals( "current", stack.getCurrent() );
		assertEquals( "previous", stack.getPrevious() );
	}
}
