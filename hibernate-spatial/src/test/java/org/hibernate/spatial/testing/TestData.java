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

	public int size() {
		return testDataElements.size();
	}

	public boolean isEmpty() {
		return testDataElements.isEmpty();
	}

	public boolean contains(Object o) {
		return testDataElements.contains( o );
	}

	public Iterator<TestDataElement> iterator() {
		return testDataElements.iterator();
	}

	public Object[] toArray() {
		return testDataElements.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return testDataElements.toArray( a );
	}

	public boolean add(TestDataElement testDataElement) {
		return testDataElements.add( testDataElement );
	}

	public boolean remove(Object o) {
		return testDataElements.remove( o );
	}

	public boolean containsAll(Collection<?> c) {
		return testDataElements.containsAll( c );
	}

	public boolean addAll(Collection<? extends TestDataElement> c) {
		return testDataElements.addAll( c );
	}

	public boolean addAll(int index, Collection<? extends TestDataElement> c) {
		return testDataElements.addAll( index, c );
	}

	public boolean removeAll(Collection<?> c) {
		return testDataElements.removeAll( c );
	}

	public boolean retainAll(Collection<?> c) {
		return testDataElements.retainAll( c );
	}

	public void clear() {
		testDataElements.clear();
	}

	public boolean equals(Object o) {
		return testDataElements.equals( o );
	}

	public int hashCode() {
		return testDataElements.hashCode();
	}

	public TestDataElement get(int index) {
		return testDataElements.get( index );
	}

	public TestDataElement set(int index, TestDataElement element) {
		return testDataElements.set( index, element );
	}

	public void add(int index, TestDataElement element) {
		testDataElements.add( index, element );
	}

	public TestDataElement remove(int index) {
		return testDataElements.remove( index );
	}

	public int indexOf(Object o) {
		return testDataElements.indexOf( o );
	}

	public int lastIndexOf(Object o) {
		return testDataElements.lastIndexOf( o );
	}

	public ListIterator<TestDataElement> listIterator() {
		return testDataElements.listIterator();
	}

	public ListIterator<TestDataElement> listIterator(int index) {
		return testDataElements.listIterator( index );
	}

	public List<TestDataElement> subList(int fromIndex, int toIndex) {
		return testDataElements.subList( fromIndex, toIndex );
	}

}
