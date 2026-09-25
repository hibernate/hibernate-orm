package org.hibernate.orm.post.fixture.validation.internal;

import org.hibernate.SPI;

/// Proves that a direct SPI annotation overrides the internal package
/// convention.
///
/// @author Steve Ebersole
@SPI
public interface InternalPackageContract {
}
