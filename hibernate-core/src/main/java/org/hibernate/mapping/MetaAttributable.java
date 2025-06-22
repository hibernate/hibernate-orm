/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;


import java.util.Map;

/**
 * Common interface for things that can handle meta attributes.
 *
 * @since 3.0.1
 */
public interface MetaAttributable {

	Map<String, MetaAttribute> getMetaAttributes();

	void setMetaAttributes(Map<String, MetaAttribute> metas);

	MetaAttribute getMetaAttribute(String name);

}
