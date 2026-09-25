package org.hibernate.boot.jaxb.mapping.spi;

import jakarta.annotation.Nullable;

/**
 * @author Steve Ebersole
 */
public interface JaxbEmbeddable extends JaxbManagedType {
	@Nullable
	String getName();
	void setName(@Nullable String name);
}
