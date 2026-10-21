package org.hibernate.boot.jaxb.mapping.spi;

/**
 * Non-id, non-version singular attribute
 *
 * @author Steve Ebersole
 */
public interface JaxbLockableAttribute extends JaxbPersistentAttribute {
	@Deprecated(since = "8.0", forRemoval = true)
	Boolean isOptimisticLock();
	@Deprecated(since = "8.0", forRemoval = true)
	void setOptimisticLock(Boolean value);

	JaxbEmptyTypeImpl getExcludeFromVersioning();
	void setExcludeFromVersioning(JaxbEmptyTypeImpl value);
}
