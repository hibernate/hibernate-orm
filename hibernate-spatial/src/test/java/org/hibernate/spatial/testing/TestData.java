/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A <code>TestData</code> instance is a list object
 * that contains all the <code>TestDataElement</code>s that
 * are used in a unit testsuite-suite suite.
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class TestData implements List<TestDataElement> {

	private List<TestDataElement> testDataElements;

	protected TestData() {
	}

	;

	public static TestData fromFile(String fileName) {
		TestDataReader reader = new TestDataReader();
		return fromFile( fileName, reader );
	}

	public static TestData fromFile(String fileName, TestDataReader reader) {
		List<TestDataElement> elements = reader.read( fileName );
		TestData testData = new TestData();
		testData.testDataElements = elements;
		return testData;
	}

	@Override
	public int size() {
		return testDataElements.size();
	}

	@Override
	public boolean isEmpty() {
		return testDataElements.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return testDataElements.contains( o );
	}

	@Override
	public Iterator<TestDataElement> iterator() {
		return testDataElements.iterator();
	}

	@Override
	public Object[] toArray() {
		return testDataElements.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return testDataElements.toArray( a );
	}

	@Override
	public boolean add(TestDataElement testDataElement) {
		return testDataElements.add( testDataElement );
	}

	@Override
	public boolean remove(Object o) {
		return testDataElements.remove( o );
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return testDataElements.containsAll( c );
	}

	@Override
	public boolean addAll(Collection<? extends TestDataElement> c) {
		return testDataElements.addAll( c );
	}

	@Override
	public boolean addAll(int index, Collection<? extends TestDataElement> c) {
		return testDataElements.addAll( index, c );
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return testDataElements.removeAll( c );
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return testDataElements.retainAll( c );
	}

	@Override
	public void clear() {
		testDataElements.clear();
	}

	@Override
	public boolean equals(Object o) {
		return testDataElements.equals( o );
	}

	@Override
	public int hashCode() {
		return testDataElements.hashCode();
	}

	@Override
	public TestDataElement get(int index) {
		return testDataElements.get( index );
	}

	@Override
	public TestDataElement set(int index, TestDataElement element) {
		return testDataElements.set( index, element );
	}

	@Override
	public void add(int index, TestDataElement element) {
		testDataElements.add( index, element );
	}

	@Override
	public TestDataElement remove(int index) {
		return testDataElements.remove( index );
	}

	@Override
	public int indexOf(Object o) {
		return testDataElements.indexOf( o );
	}

	@Override
	public int lastIndexOf(Object o) {
		return testDataElements.lastIndexOf( o );
	}

	@Override
	public ListIterator<TestDataElement> listIterator() {
		return testDataElements.listIterator();
	}

	@Override
	public ListIterator<TestDataElement> listIterator(int index) {
		return testDataElements.listIterator( index );
	}

	@Override
	public List<TestDataElement> subList(int fromIndex, int toIndex) {
		return testDataElements.subList( fromIndex, toIndex );
	}

}
