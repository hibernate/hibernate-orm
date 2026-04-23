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
package org.hibernate.tool.internal.util;

import jakarta.persistence.DiscriminatorType;

import org.hibernate.annotations.FlushModeType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleWithSubselectEnum;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;

public final class HbmEnumMapper {

	private HbmEnumMapper() {
	}

	public static CacheConcurrencyStrategy mapCacheConcurrency(AccessType usage) {
		return switch (usage) {
			case READ_ONLY -> CacheConcurrencyStrategy.READ_ONLY;
			case READ_WRITE -> CacheConcurrencyStrategy.READ_WRITE;
			case NONSTRICT_READ_WRITE -> CacheConcurrencyStrategy.NONSTRICT_READ_WRITE;
			case TRANSACTIONAL -> CacheConcurrencyStrategy.TRANSACTIONAL;
		};
	}

	public static OptimisticLockType mapOptimisticLockType(OptimisticLockStyle style) {
		return switch (style) {
			case NONE -> OptimisticLockType.NONE;
			case DIRTY -> OptimisticLockType.DIRTY;
			case ALL -> OptimisticLockType.ALL;
			default -> OptimisticLockType.VERSION;
		};
	}

	public static FetchMode mapFetchMode(JaxbHbmFetchStyleWithSubselectEnum fetch) {
		return switch (fetch) {
			case JOIN -> FetchMode.JOIN;
			case SELECT -> FetchMode.SELECT;
			case SUBSELECT -> FetchMode.SUBSELECT;
		};
	}

	public static FlushModeType mapFlushMode(org.hibernate.FlushMode flushMode) {
		return switch (flushMode) {
			case AUTO -> FlushModeType.AUTO;
			case ALWAYS -> FlushModeType.ALWAYS;
			case COMMIT -> FlushModeType.COMMIT;
			case MANUAL -> FlushModeType.MANUAL;
			default -> FlushModeType.PERSISTENCE_CONTEXT;
		};
	}

	public static DiscriminatorType mapAnyDiscriminatorType(String metaType) {
		if (metaType == null) {
			return DiscriminatorType.STRING;
		}
		return switch (metaType.toLowerCase()) {
			case "integer", "int", "long", "short" -> DiscriminatorType.INTEGER;
			case "character", "char" -> DiscriminatorType.CHAR;
			default -> DiscriminatorType.STRING;
		};
	}
}
