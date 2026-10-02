package org.hibernate.boot.jaxb.mapping.spi;

/**
 * Non-id, non-version singular attribute
 *
 * @author Steve Ebersole
 */
public interface JaxbLockableAttribute extends JaxbPersistentAttribute {
	Boolean isOptimisticLock();
	void setOptimisticLock(Boolean value);
}
