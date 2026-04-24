/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
