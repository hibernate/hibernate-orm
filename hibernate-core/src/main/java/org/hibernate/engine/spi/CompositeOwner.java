package org.hibernate.engine.spi;

/**
 * @author Ståle W. Pedersen
 */
public interface CompositeOwner extends PrimeAmongSecondarySupertypes {
	/**
	 * @param attributeName to be added to the dirty list
	 */
	void $$_hibernate_trackChange(String attributeName);

	@Override
	default CompositeOwner asCompositeOwner() {
		return this;
	}

}
