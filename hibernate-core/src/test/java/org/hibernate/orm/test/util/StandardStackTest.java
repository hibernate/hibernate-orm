/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import java.util.NoSuchElementException;

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
public class StandardStackTest {
	@Test
	public void testSimpleStackAccess() {
		final Stack<Integer> stack = allocateStack( 5 );
		assertEquals( 5, stack.depth() );
		assertEquals( 0, stack.getRoot() );
		assertEquals( 4, stack.getCurrent() );
		assertEquals( 4, stack.pop() );
		assertEquals( 4, stack.depth() );
	}

	@Test
	public void testNullValues() {
		final Stack<Integer> stack = allocateStack( 1 );
		stack.push( null );
		assertNull( stack.getCurrent() );
		assertNull( stack.pop() );
		assertNotNull( stack.getCurrent() );
	}

	@Test
	public void testVisitRootFirst() {
		final Stack<Integer> stack = allocateStack( 5 );
		final int[] i = { 0 };
		stack.visitRootFirst( value -> {
			assertEquals( i[0], value );
			i[0]++;
		} );
	}

	@Test
	public void testFindCurrentFirst() {
		final Stack<Integer> stack = allocateStack( 5 );
		final Integer result = stack.findCurrentFirst( value -> value == 1 ? value : null );
		assertEquals( 1, result );
		final Integer nullResult = stack.findCurrentFirst( value -> value == 42 ? value : null );
		assertNull( nullResult );
	}

	@Test
	public void testFindCurrentFirstWithParameter() {
		final Stack<Integer> stack = allocateStack( 5 );
		final Integer result = stack.findCurrentFirstWithParameter( 1, this::returnIfEquals );
		assertEquals( 1, result );
		final Integer nullResult = stack.findCurrentFirstWithParameter( 42, this::returnIfEquals );
		assertNull( nullResult );
	}

	// empty stack tests

	@Test
	public void testEmptyStackAccess() {
		final Stack<Integer> emptyStack = allocateStack( 0 );
		assertTrue( emptyStack.isEmpty() );
		assertNull( emptyStack.getRoot() );
		assertNull( emptyStack.getCurrent() );
		assertEquals( 0, emptyStack.depth() );
		assertThrows( NoSuchElementException.class, emptyStack::pop );
	}

	@Test
	public void testVisitRootFirstEmpty() {
		final Stack<Integer> emptyStack = allocateStack( 0 );
		final int[] i = { 0 };
		emptyStack.visitRootFirst( value -> i[0]++ );
		assertEquals( 0, i[0] ); // lambda function should never have been invoked
	}

	@Test
	public void testFindCurrentFirstEmpty() {
		final Stack<Integer> emptyStack = allocateStack( 0 );
		final Integer result = emptyStack.findCurrentFirst( value -> value );
		assertNull( result );
	}

	@Test
	public void testFindCurrentFirstWithParameterEmpty() {
		final Stack<Integer> emptyStack = allocateStack( 0 );
		final Integer result = emptyStack.findCurrentFirstWithParameter( 1, (value, param) -> value );
		assertNull( result );
	}

	// cleared stack tests

	@Test
	public void testClear() {
		final Stack<Integer> clearedStack = allocateStack( 42 );
		assertEquals( 42, clearedStack.depth() );
		assertFalse( clearedStack.isEmpty() );
		clearedStack.clear();
		assertTrue( clearedStack.isEmpty() );
		assertNull( clearedStack.getRoot() );
		assertNull( clearedStack.getCurrent() );
		assertEquals( 0, clearedStack.depth() );
		assertThrows( NoSuchElementException.class, clearedStack::pop );
	}

	@Test
	public void testVisitRootFirstCleared() {
		final Stack<Integer> clearedStack = allocateStack( 5 );
		clearedStack.clear();
		final int[] i = { 0 };
		clearedStack.visitRootFirst( value -> i[0]++ );
		assertEquals( 0, i[0] ); // lambda function should never have been run
	}

	@Test
	public void testFindCurrentFirstCleared() {
		final Stack<Integer> clearedStack = allocateStack( 5 );
		clearedStack.clear();
		final Integer result = clearedStack.findCurrentFirst( value -> value );
		assertNull( result );
	}

	@Test
	public void testFindCurrentFirstWithParameterCleared() {
		final Stack<Integer> clearedStack = allocateStack( 5 );
		clearedStack.clear();
		final Integer result = clearedStack.findCurrentFirstWithParameter( 1, (value, param) -> value );
		assertNull( result );
	}

	// utility functions

	private Stack<Integer> allocateStack(int size) {
		final Stack<Integer> stack = new StandardStack<>();
		for ( int i = 0; i < size; i++ ) {
			stack.push( i );
		}
		return stack;
	}

	private Integer returnIfEquals(Integer value, Integer param) {
		return value.equals( param ) ? value : null;
	}
}
