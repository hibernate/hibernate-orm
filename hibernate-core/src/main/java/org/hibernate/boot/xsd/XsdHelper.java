/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.xsd;

/**
 * @author Steve Ebersole
 */
public class XsdHelper {
	public static boolean isValidJpaVersion(String version) {
		return switch ( version ) {
			case "1.0", "2.0", "2.1", "2.2", "3.0", "3.1", "3.2", "7.0" -> true;
			default -> false;
		};
	}

	public static boolean shouldBeMappedToLatestJpaDescriptor(String uri) {
		// JPA 1.0 and 2.0 share the same namespace URI
		// JPA 2.1 and 2.2 share the same namespace URI
		return MappingXsdSupport.jpa10.getNamespaceUri().equals( uri );
	}
}
