/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JaxbQueryHintContainer {
	String getName();
	String getDescription();
	List<? extends JaxbQueryHint> getHints();
}
