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
	public long getNextValue();
}
