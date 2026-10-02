package org.hibernate.persister.walking.spi;

import jakarta.annotation.Nonnull;

/**
* @author Steve Ebersole
*/
public interface AttributeSource {
	default int getPropertyIndex(@Nonnull String propertyName) {
		throw new UnsupportedOperationException();
	}
}
