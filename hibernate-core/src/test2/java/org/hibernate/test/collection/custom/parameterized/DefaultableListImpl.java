/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.parameterized;

import java.util.ArrayList;

/**
 * Implementation of our specialized collection contract
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public class DefaultableListImpl extends ArrayList implements DefaultableList {
    private String defaultValue;

	public DefaultableListImpl() {
	}

	public DefaultableListImpl(int anticipatedSize) {
		super( anticipatedSize + ( int ) Math.ceil( anticipatedSize * .75f ) );
	}

	public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
