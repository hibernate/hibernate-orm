package org.hibernate.test.usercollection.parameterized;

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
