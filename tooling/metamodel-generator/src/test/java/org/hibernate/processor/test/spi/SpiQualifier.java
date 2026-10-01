/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.spi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** The qualifier {@link RecordingExtension} asks the processor to put on injected sessions. */
@Retention(RetentionPolicy.RUNTIME)
public @interface SpiQualifier {
	String value();
}
