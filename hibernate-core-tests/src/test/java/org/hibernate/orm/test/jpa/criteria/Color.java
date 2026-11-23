package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class Color implements Attribute {
	public static String TYPE = "Color";

	@Override
	public String getType() {
		return TYPE;
	}
}
