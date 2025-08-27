/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;


/**
 * Enumeration of the known places from which a piece of metadata may come.
 *
 * @author Steve Ebersole
 */
public enum MetadataSource {
	HBM,
	ANNOTATIONS,
	OTHER
}
