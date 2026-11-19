package org.hibernate.id.enhanced;

/**
 * Contract for providing callback access to a {@link DatabaseStructure},
 * typically from the {@link Optimizer}.
 *
 * @author Steve Ebersole
 */
public interface AccessCallback {
	/**
	 * Retrieve the next value from the underlying source.
	 *
	 * @return The next value.
	 */
	long getNextValue();
	/**
	 * Retrieve the next values from the underlying source.
	 *
	 * @return The next values.
	 */
	default long[] getNextValues(int count) {
		final long[] result = new long[count];
		for (int i = 0; i < count; i++) {
			result[i] = getNextValue();
		}
		return result;
	}

	/**
	 * Obtain the tenant identifier (multi-tenancy), if one, associated with this callback.
	 *
	 * @return The tenant identifier
	 */
	String getTenantIdentifier();
}
