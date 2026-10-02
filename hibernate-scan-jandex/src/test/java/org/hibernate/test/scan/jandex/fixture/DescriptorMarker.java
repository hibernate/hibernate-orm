package org.hibernate.test.scan.jandex.fixture;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.persistence.spi.Discoverable;

import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Marks package and module descriptors for scanner fixtures.
///
/// @author Steve Ebersole
@Discoverable
@Retention(RUNTIME)
@Target({ MODULE, PACKAGE })
public @interface DescriptorMarker {
}
