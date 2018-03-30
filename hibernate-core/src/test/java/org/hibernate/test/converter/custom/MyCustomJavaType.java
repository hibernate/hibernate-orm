/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.custom;

/**
 * @author Steve Ebersole
 */
public class MyCustomJavaType implements Comparable<MyCustomJavaType> {
	private String payload;

	public MyCustomJavaType() {
		this( null );
	}

	public MyCustomJavaType(String payload) {
		this.payload = payload;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	@Override
	public int compareTo(MyCustomJavaType other) {
		if ( getPayload() == null ) {
			return -1;
		}

		if ( other == null || other.getPayload() == null ) {
			return 1;
		}

		return getPayload().compareTo( other.getPayload() );
	}
}
