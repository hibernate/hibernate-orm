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
public class NestedCtorLookupListItem extends ConstructedLookupListItem implements NestedLookupListItem {
	private final LookupListItem nested;

	public NestedCtorLookupListItem(
			Integer id,
			String displayValue,
			LookupListItem nested) {
		super( id, displayValue );
		this.nested = nested;
	}

	@Override
	public LookupListItem getNested() {
		return nested;
	}
}
