/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.GeneratedValue;

/// Binding phase for identifier generator resolution.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierGeneratorBindingPhase {
	public static void resolveGeneratedValueGenerator(
			PersistentClass persistentClass,
			SimpleValue idValue,
			MemberDetails idMember,
			GeneratedValue generatedValue,
			MetadataBuildingContext context) {
		GeneratorBinder.resolveGeneratedValueGenerator(
				persistentClass,
				idValue,
				idMember,
				generatedValue,
				context
		);
	}

	public interface Resolution {
		void resolveIdentifierGenerator();
	}
}
