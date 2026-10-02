package org.hibernate.processor.test.spi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Marks the types handled by {@link RecordingExtension}. */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionMarker {
}
