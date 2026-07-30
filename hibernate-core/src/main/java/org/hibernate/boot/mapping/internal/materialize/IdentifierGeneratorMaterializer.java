/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModelImpl;
import org.hibernate.boot.mapping.internal.categorize.IdentifierGeneratorResolution;
import org.hibernate.boot.model.IdentifierGeneratorRegistration;
import org.hibernate.boot.model.internal.GeneratorAnnotationHelper;
import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;

/// Applies identifier-generator decisions made during categorization.
///
/// @since 9.0
/// @author Steve Ebersole
final class IdentifierGeneratorMaterializer {
	private IdentifierGeneratorMaterializer() {
	}

	static IdentifierGeneratorResolution.Part findResolution(
			MemberDetails member,
			RootClass typeBinding,
			CategorizedDomainModelImpl categorizedDomainModel) {
		for ( var hierarchy : categorizedDomainModel.getEntityHierarchies() ) {
			if ( hierarchy.getRoot().getEntityName().equals( typeBinding.getEntityName() ) ) {
				return hierarchy.getIdentifierGeneratorResolution().find( member );
			}
		}
		throw new AssertionFailure(
				"Could not locate categorized hierarchy for entity '" + typeBinding.getEntityName() + "'"
		);
	}

	static void apply(
			IdentifierGeneratorResolution.Part resolution,
			SimpleValue idValue,
			MemberDetails member,
			MetadataBuildingContext buildingContext) {
		if ( resolution == null ) {
			return;
		}

		if ( resolution.nature() == IdentifierGeneratorResolution.Nature.IDENTITY ) {
			GeneratorAnnotationHelper.handleIdentityStrategy( idValue );
			return;
		}

		if ( resolution.configuration() != null ) {
			if ( resolution.configuration() instanceof jakarta.persistence.SequenceGenerator sequenceGenerator ) {
				GeneratorAnnotationHelper.handleSequenceGenerator(
						resolution.registration().getName(),
						sequenceGenerator,
						idValue,
						member,
						buildingContext
				);
			}
			else if ( resolution.configuration() instanceof jakarta.persistence.TableGenerator tableGenerator ) {
				GeneratorAnnotationHelper.handleTableGenerator(
						resolution.registration().getName(),
						tableGenerator,
						idValue,
						member,
						buildingContext
				);
			}
			else if ( resolution.configuration() instanceof UuidGenerator uuidGenerator ) {
				idValue.setCustomIdGeneratorCreator(
						GeneratorAnnotationHelper.uuidGeneratorDescriptor( uuidGenerator )
				);
			}
			else {
				idValue.setCustomIdGeneratorCreator(
						GeneratorBinder.identifierGeneratorDescriptor( resolution.configuration() )
				);
			}
			return;
		}

		if ( resolution.registration().getKind() == IdentifierGeneratorRegistration.Kind.UUID ) {
			idValue.setCustomIdGeneratorCreator( GeneratorAnnotationHelper.uuidGeneratorDescriptor( null ) );
		}
		else {
			GeneratorBinder.createGeneratorFrom( resolution.registration(), idValue, buildingContext );
		}
	}
}
