/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.properties;

import java.util.Comparator;

/**
 * Sorts {@link SettingDescriptor} references by setting name with the following precedence - <ol>
 *     <li>starts with {@code jakarta.persistence.}</li>
 *     <li>starts with {@code hibernate.}</li>
 *     <li>starts with {@code javax.persistence.}</li>
 *     <li>any others (should be none)</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class SettingDescriptorComparator implements Comparator<SettingDescriptor> {
	public static final SettingDescriptorComparator INSTANCE = new SettingDescriptorComparator();

	public static final String JPA_PREFIX = "jakarta.persistence.";
	public static final String HIBERNATE_PREFIX = "hibernate.";
	public static final String LEGACY_JPA_PREFIX = "javax.persistence.";

	@Override
	public int compare(SettingDescriptor o1, SettingDescriptor o2) {
		if ( o1.getName().startsWith( JPA_PREFIX ) ) {
			if ( o2.getName().startsWith( JPA_PREFIX ) ) {
				return o1.getName().compareTo( o2.getName() );
			}
			return -1;
		}

		if ( o1.getName().startsWith( HIBERNATE_PREFIX ) ) {
			if ( o2.getName().startsWith( JPA_PREFIX ) ) {
				return 1;
			}
			if ( o2.getName().startsWith( HIBERNATE_PREFIX ) ) {
				return o1.getName().compareTo( o2.getName() );
			}
			return -1;
		}

		assert o1.getName().startsWith( LEGACY_JPA_PREFIX );

		if ( o2.getName().startsWith( JPA_PREFIX )
				|| o2.getName().startsWith( HIBERNATE_PREFIX ) ) {
			return 1;
		}

		return o1.getName().compareTo( o2.getName() );
	}
}
