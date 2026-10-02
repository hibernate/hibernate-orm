package org.hibernate.envers.boot.internal;

/**
 * @author Chris Cranford
 */
public class ModifiedColumnNameResolver {
	public static String getName(String propertyName, String suffix) {
		return propertyName + suffix;
	}

	private ModifiedColumnNameResolver() {
	}
}
