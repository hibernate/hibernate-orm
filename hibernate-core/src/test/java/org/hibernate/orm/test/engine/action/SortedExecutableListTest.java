/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ComparableExecutable;
import org.hibernate.engine.spi.ExecutableList;
import org.hibernate.event.spi.EventSource;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anton Marsden
 */
@BaseUnitTest
public class SortedExecutableListTest {

	// For testing, we need an Executable that is also Comparable and Serializable
	private static class AnExecutable implements ComparableExecutable {

		private final int n;
		private String[] spaces;
		private transient boolean afterDeserializeCalled;

		public AnExecutable(int n, String... spaces) {
			this.n = n;
			this.spaces = spaces;
		}

		public boolean wasAfterDeserializeCalled() {
			return afterDeserializeCalled;
		}

		@Override
		public int compareTo(ComparableExecutable o) {
			Integer index = (Integer) o.getSecondarySortIndex();
			return Integer.compare( n, index.intValue() );
		}

		@Override
		public int hashCode() {
			return n;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			AnExecutable other = (AnExecutable) obj;
			return n == other.n;
		}

		@Override
		public String[] getPropertySpaces() {
			return spaces;
		}

		@Override
		public void beforeExecutions() throws HibernateException {
		}

		@Override
		public void execute() throws HibernateException {
		}

		@Override
		public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
			return null;
		}

		@Override
		public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
			return null;
		}

		@Override
		public void afterDeserialize(EventSource session) {
			this.afterDeserializeCalled = true;
		}

		public String toString() {
			return String.valueOf( n );
		}

		@Override
		public String getPrimarySortClassifier() {
			return toString();
		}

		@Override
		public Object getSecondarySortIndex() {
			return Integer.valueOf( n );
		}
	}

	private ExecutableList<AnExecutable> actionList;

	private final AnExecutable action1 = new AnExecutable( 0, "a" );
	private final AnExecutable action2 = new AnExecutable( 1, "b", "c" );
	private final AnExecutable action3 = new AnExecutable( 2, "b", "d" );
	private final AnExecutable action4 = new AnExecutable( 3 );

	@BeforeEach
	public void setUp() {
		actionList = new ExecutableList<>();
	}

	@AfterEach
	public void tearDown() {
		actionList = null;
	}

	@Test
	public void testAdd() {
		assertThat( actionList ).isEmpty();

		actionList.add( action1 );
		assertThat( actionList ).hasSize( 1 );
		assertThat( actionList ).element( 0 ).isSameAs( action1 );

		actionList.add( action3 );
		assertThat( actionList ).hasSize( 2 );
		assertThat( actionList ).element( 0 ).isSameAs( action1 );
		assertThat( actionList ).element( 1 ).isSameAs( action3 );
	}

	@Test
	public void testClear() {
		assertThat( actionList ).isEmpty();

		actionList.add( action1 );
		assertThat( actionList ).hasSize( 1 );

		actionList.add( action2 );
		assertThat( actionList ).hasSize( 2 );

		actionList.clear();

		assertThat( actionList ).isEmpty();
	}

	@Test
	public void testIterator() {
		actionList.add( action1 );
		actionList.add( action2 );
		actionList.add( action3 );

		final Iterator<AnExecutable> iterator = actionList.iterator();
		assertThat( iterator.next() ).isEqualTo( action1 );
		assertThat( iterator.next() ).isEqualTo( action2 );
		assertThat( iterator.next() ).isEqualTo( action3 );
		assertThat( iterator.hasNext() ).isEqualTo( false );
	}

	@Test
	public void testRemoveLastN() {
		actionList.add( action1 );
		actionList.add( action2 );
		actionList.add( action3 );

		assertThat( actionList ).hasSize( 3 );

		actionList.removeLastN( 0 );
		assertThat( actionList ).hasSize( 3 );

		actionList.removeLastN( 2 );
		assertThat( actionList ).hasSize( 1 );
		assertThat( actionList ).element( 0 ).isSameAs( action1 );
	}

	@Test
	public void testGetSpaces() {
		actionList.add( action1 );

		final Set<Serializable> initialQuerySpaces = actionList.getQuerySpaces();
		assertThat( initialQuerySpaces ).containsOnly( "a" );

		actionList.add( action2 );
		actionList.add( action3 );
		actionList.add( action4 );

		{
			final Set<Serializable> spaces = actionList.getQuerySpaces();
			assertThat( spaces ).isSameAs( initialQuerySpaces );
			assertThat( spaces ).hasSize( 4 );
			assertThat( spaces ).containsOnly( "a", "b", "c", "d" );
		}

		// now remove action4
		actionList.remove( 3 );

		{
			final Set<Serializable> spaces = actionList.getQuerySpaces();
			// same Set (action4 has no spaces)
			assertThat( spaces ).isSameAs( initialQuerySpaces );
			assertThat( spaces ).containsAll( initialQuerySpaces );
			assertThat( spaces ).hasSize( 4 );
		}

		actionList.remove( 2 );

		{
			final Set<Serializable> spaces = actionList.getQuerySpaces();
			// Different Set because it has been rebuilt. This would be incorrect if Set.clear() was used
			assertThat( spaces ).isNotSameAs( initialQuerySpaces );
			assertThat( spaces ).hasSize( 3 );
		}
	}

	@Test
	public void testSort() {
		actionList.add( action4 );
		actionList.add( action3 );
		actionList.add( action2 );
		actionList.add( action1 );

		assertThat( actionList ).element( 0 ).isSameAs( action4 );
		assertThat( actionList ).element( 1 ).isSameAs( action3 );
		assertThat( actionList ).element( 2 ).isSameAs( action2 );
		assertThat( actionList ).element( 3 ).isSameAs( action1 );

		actionList.sort();

		assertThat( actionList ).element( 0 ).isSameAs( action1 );
		assertThat( actionList ).element( 1 ).isSameAs( action2 );
		assertThat( actionList ).element( 2 ).isSameAs( action3 );
		assertThat( actionList ).element( 3 ).isSameAs( action4 );
	}

	@Test
	public void testSerializeDeserialize() throws IOException, ClassNotFoundException {
		actionList.add( action4 );
		actionList.add( action3 );
		actionList.add( action2 );
		actionList.add( action1 );

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream( baos );
		actionList.writeExternal( oos );
		oos.flush();
		final ByteArrayInputStream bin = new ByteArrayInputStream( baos.toByteArray() );
		final ObjectInputStream ois = new ObjectInputStream( bin );
		actionList = new ExecutableList<>();
		actionList.readExternal( ois );

		assertThat( actionList ).hasSize( 4 );
		assertThat( actionList ).element( 0 ).isEqualTo( action4 );
		assertThat( actionList ).element( 1 ).isEqualTo( action3 );
		assertThat( actionList ).element( 2 ).isEqualTo( action2 );
		assertThat( actionList ).element( 3 ).isEqualTo( action1 );

		assertThat( actionList ).element( 0 ).extracting( AnExecutable::wasAfterDeserializeCalled ).isEqualTo( false );
		assertThat( actionList ).element( 1 ).extracting( AnExecutable::wasAfterDeserializeCalled ).isEqualTo( false );
		assertThat( actionList ).element( 2 ).extracting( AnExecutable::wasAfterDeserializeCalled ).isEqualTo( false );
		assertThat( actionList ).element( 3 ).extracting( AnExecutable::wasAfterDeserializeCalled ).isEqualTo( false );

		actionList.afterDeserialize( null );

		assertThat( actionList ).hasSize( 4 );
		assertThat( actionList ).element( 0 ).isEqualTo( action4 );
		assertThat( actionList ).element( 1 ).isEqualTo( action3 );
		assertThat( actionList ).element( 2 ).isEqualTo( action2 );
		assertThat( actionList ).element( 3 ).isEqualTo( action1 );

		assertThat( actionList ).element( 0 ).extracting( AnExecutable::wasAfterDeserializeCalled ).isEqualTo( true );
		assertThat( actionList ).element( 1 ).extracting( AnExecutable::wasAfterDeserializeCalled ).isEqualTo( true );
		assertThat( actionList ).element( 2 ).extracting( AnExecutable::wasAfterDeserializeCalled ).isEqualTo( true );
		assertThat( actionList ).element( 3 ).extracting( AnExecutable::wasAfterDeserializeCalled ).isEqualTo( true );

		actionList.sort();

		assertThat( actionList ).element( 0 ).isEqualTo( action1 );
		assertThat( actionList ).element( 1 ).isEqualTo( action2 );
		assertThat( actionList ).element( 2 ).isEqualTo( action3 );
		assertThat( actionList ).element( 3 ).isEqualTo( action4 );
	}
}
