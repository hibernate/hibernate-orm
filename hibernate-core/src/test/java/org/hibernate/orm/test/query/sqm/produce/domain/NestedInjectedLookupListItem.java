/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce.domain;

/**
 * @author Steve Ebersole
 */
public class NestedInjectedLookupListItem extends InjectedLookupListItem implements NestedLookupListItem {
	private LookupListItem nested;

	public void setNested(LookupListItem nested) {
		this.nested = nested;
	}

	@Override
	public LookupListItem getNested() {
		return nested;
	}
}
