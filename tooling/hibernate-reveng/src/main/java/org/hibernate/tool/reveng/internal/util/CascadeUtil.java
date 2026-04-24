/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import jakarta.persistence.CascadeType;

import org.hibernate.annotations.Cascade;

public final class CascadeUtil {

	private CascadeUtil() {
	}

	public static String toHbmCascade(CascadeType cascadeType) {
		return switch (cascadeType) {
			case ALL -> "all";
			case PERSIST -> "persist";
			case MERGE -> "merge";
			case REMOVE -> "delete";
			case REFRESH -> "refresh";
			case DETACH -> "evict";
		};
	}

	public static String toHbmHibernateCascade(org.hibernate.annotations.CascadeType cascadeType) {
		return switch (cascadeType) {
			case ALL -> "all";
			case PERSIST -> "persist";
			case MERGE -> "merge";
			case REMOVE -> "delete";
			case REFRESH -> "refresh";
			case DETACH -> "evict";
			case LOCK -> "lock";
			case REPLICATE -> "replicate";
			case DELETE_ORPHAN -> "delete-orphan";
		};
	}

	public static String formatJpaCascade(CascadeType[] cascadeTypes) {
		if (cascadeTypes == null || cascadeTypes.length == 0) {
			return "none";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cascadeTypes.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(toHbmCascade(cascadeTypes[i]));
		}
		return sb.toString();
	}

	public static String formatHibernateCascade(Cascade cascade) {
		if (cascade == null || cascade.value().length == 0) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cascade.value().length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(toHbmHibernateCascade(cascade.value()[i]));
		}
		return sb.toString();
	}
}
