/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * Contract for JAXB bindings which are containers of tooling hints.
 *
 * @author Steve Ebersole
 */
public interface ToolingHintContainer {
	List<JaxbHbmToolingHintType> getToolingHints();
}
