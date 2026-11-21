package org.hibernate.jpa.internal.enhance;

import java.util.Map;

import org.hibernate.jpa.HibernatePersistenceProvider;

import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;

/// Fallback transformer registration using the provider's enhancement pipeline.
///
/// @author Steve Ebersole
public final class ContainerEnhancement {
	private ContainerEnhancement() {
	}

	public static ClassTransformer createTransformer(PersistenceUnitInfo unit, Map<?, ?> settings) {
		return new HibernatePersistenceProvider().getClassTransformer( unit, settings );
	}
}
