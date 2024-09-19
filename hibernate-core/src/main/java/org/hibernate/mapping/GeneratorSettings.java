/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;


/**
 * @author Gavin King
 */
public interface GeneratorSettings {
	String getDefaultCatalog();
	String getDefaultSchema();
}
