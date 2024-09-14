/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf;

//tag::embeddable-instantiator-property[]
public class NameImpl implements Name {
	private final String first;
	private final String last;

	private NameImpl() {
		throw new UnsupportedOperationException();
	}

	public NameImpl(String first, String last) {
		this.first = first;
		this.last = last;
	}

	@Override
	public String getFirstName() {
		return first;
	}

	@Override
	public String getLastName() {
		return last;
	}
}
//end::embeddable-instantiator-property[]
