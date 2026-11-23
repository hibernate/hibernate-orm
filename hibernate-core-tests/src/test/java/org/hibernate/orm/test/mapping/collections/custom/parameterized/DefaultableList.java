package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import java.util.List;

/**
 * Our specialized collection contract
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public interface DefaultableList extends List {
	public String getDefaultValue();
}
