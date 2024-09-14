/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.merge;

public class MyCompositeValue {
	protected Long longValue;
	protected String stringValue;

	public MyCompositeValue() {
	}

	public MyCompositeValue(Long longValue, String stringValue) {
		this.longValue = longValue;
		this.stringValue = stringValue;
	}

	public Long longValue() {
		return longValue;
	}

	public String stringValue() {
		return stringValue;
	}

}
