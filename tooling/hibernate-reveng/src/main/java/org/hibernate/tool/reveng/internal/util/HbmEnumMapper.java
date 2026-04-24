/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

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
