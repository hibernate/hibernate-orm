package org.hibernate.boot.registry.selector.spi;

import org.hibernate.dialect.Dialect;

/// Minimal Dialect-selector fixture.
///
/// @author Steve Ebersole
public interface DialectSelector {
	Class<? extends Dialect> resolve(String name);
}
