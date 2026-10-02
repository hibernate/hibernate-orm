package org.hibernate.orm.test.query.sqm.domain;

/**
 * @author Steve Ebersole
 */
public interface NestedLookupListItem extends LookupListItem {
	LookupListItem getNested();
}
