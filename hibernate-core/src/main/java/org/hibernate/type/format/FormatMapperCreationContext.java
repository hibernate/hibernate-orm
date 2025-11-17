/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.Incubating;
import org.hibernate.boot.spi.BootstrapContext;


/**
 * The creation context for {@link FormatMapper} that is passed as constructor argument to implementations.
 */
@Incubating
public interface FormatMapperCreationContext {
	BootstrapContext getBootstrapContext();

}
