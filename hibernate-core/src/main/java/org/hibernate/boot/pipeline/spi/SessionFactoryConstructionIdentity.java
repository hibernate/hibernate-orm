/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.spi;

import java.util.Map;
import java.util.Objects;

import org.hibernate.cfg.PersistenceSettings;

import static org.hibernate.id.uuid.LocalObjectUuidHelper.generateLocalObjectUuid;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/// Identity assigned to the SessionFactory being constructed.
/// <p>
/// The 9.0 bootstrap pipeline intentionally resolves this without consulting
/// cfg.xml aggregation.  The legacy/native construction bridge keeps the older
/// SessionFactoryImpl name and JNDI helpers until that path is retired.
///
/// @since 9.0
/// @author Steve Ebersole
public record SessionFactoryConstructionIdentity(
		String uuid,
		String name,
		String jndiName) {

	public SessionFactoryConstructionIdentity {
		Objects.requireNonNull( uuid );
	}

	public static SessionFactoryConstructionIdentity resolve(ResolvedSessionFactorySettings settings) {
		Objects.requireNonNull( settings );
		final String explicitName = settings.sessionFactoryName();
		final String name = isNotEmpty( explicitName )
				? explicitName
				: persistenceUnitName( settings.configurationValues() );
		return new SessionFactoryConstructionIdentity(
				generateLocalObjectUuid(),
				name,
				resolveJndiName( settings, explicitName )
		);
	}

	private static String resolveJndiName(
			ResolvedSessionFactorySettings settings,
			String explicitName) {
		if ( isNotEmpty( settings.sessionFactoryJndiName() ) ) {
			return settings.sessionFactoryJndiName();
		}
		if ( !isNotEmpty( explicitName ) || settings.sessionFactoryNameAlsoJndiName() == Boolean.FALSE ) {
			return null;
		}
		return explicitName;
	}

	private static String persistenceUnitName(Map<String, Object> configurationValues) {
		final Object persistenceUnitName = configurationValues.get( PersistenceSettings.PERSISTENCE_UNIT_NAME );
		return persistenceUnitName == null ? null : persistenceUnitName.toString();
	}
}
