package org.hibernate.loader.custom;

import org.hibernate.LockMode;

/**
 * Represents a return which names a fetched association.
 *
 * @author Steve Ebersole
 */
public abstract class FetchReturn extends NonScalarReturn {
	private final NonScalarReturn owner;
	private final String ownerProperty;

	/**
	 * Creates a return descriptor for an association fetch.
	 *
	 * @param owner The return descriptor for the owner of the fetch
	 * @param ownerProperty The name of the property represernting the association being fetched
	 * @param alias The alias for the fetch
	 * @param lockMode The lock mode to apply to the fetched association.
	 */
	public FetchReturn(
			NonScalarReturn owner,
			String ownerProperty,
			String alias,
			LockMode lockMode) {
		super( alias, lockMode );
		this.owner = owner;
		this.ownerProperty = ownerProperty;
	}

	/**
	 * Retrieves the return descriptor for the owner of this fetch.
	 *
	 * @return The owner
	 */
	public NonScalarReturn getOwner() {
		return owner;
	}

	/**
	 * The name of the property on the owner which represents this association.
	 *
	 * @return The property name.
	 */
	public String getOwnerProperty() {
		return ownerProperty;
	}
}
