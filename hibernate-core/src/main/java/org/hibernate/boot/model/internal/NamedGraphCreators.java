/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.boot.model.NamedGraphCreator;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.BatchFetch;
import jakarta.persistence.FetchOption;
import jakarta.persistence.NamedEntityGraph;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Factory access to the internal named-graph creator implementations.
 *
 * @author Steve Ebersole
 */
public final class NamedGraphCreators {
	public static NamedGraphCreator jpa(
			NamedEntityGraph annotation,
			String jpaEntityName,
			ModelsContext modelsContext) {
		return new NamedGraphCreatorJpa( annotation, jpaEntityName, List.of(), modelsContext );
	}

	public static NamedGraphCreator jpa(
			NamedEntityGraph annotation,
			String jpaEntityName,
			ClassDetails classDetails,
			ModelsContext modelsContext) {
		final var fetchGraphContributions = collectFetchGraphContributions( classDetails, modelsContext );
		validateFetchGraphNames(
				fetchGraphContributions,
				classDetails.getRepeatedAnnotationUsages( NamedEntityGraph.class, modelsContext ),
				jpaEntityName
		);
		return new NamedGraphCreatorJpa( annotation, jpaEntityName, fetchGraphContributions, modelsContext );
	}

	public static NamedGraphCreator parsed(
			Class<?> entityType,
			org.hibernate.annotations.NamedEntityGraph annotation) {
		return new NamedGraphCreatorParsed( entityType, annotation );
	}

	public static NamedGraphCreator parsed(org.hibernate.annotations.NamedEntityGraph annotation) {
		return new NamedGraphCreatorParsed( annotation );
	}

	private NamedGraphCreators() {
	}

	private static List<FetchGraphContribution> collectFetchGraphContributions(
			ClassDetails classDetails,
			ModelsContext modelsContext) {
		final var contributions = new ArrayList<FetchGraphContribution>();
		collectFetchGraphContributions( classDetails.getFields(), modelsContext, contributions );
		collectFetchGraphContributions( classDetails.getMethods(), modelsContext, contributions );
		collectFetchGraphContributions( classDetails.getRecordComponents(), modelsContext, contributions );
		return contributions;
	}

	private static void collectFetchGraphContributions(
			List<? extends MemberDetails> members,
			ModelsContext modelsContext,
			List<FetchGraphContribution> contributions) {
		for ( var member : members ) {
			member.forEachRepeatedAnnotationUsages(
					JpaAnnotations.FETCH,
					modelsContext,
					usage -> {
						if ( isNotEmpty( usage.graph() ) ) {
							contributions.add( fetchGraphContribution( member, usage ) );
						}
					}
			);
		}
	}

	private static FetchGraphContribution fetchGraphContribution(MemberDetails member, jakarta.persistence.Fetch fetch) {
		return new FetchGraphContribution(
				fetch.graph(),
				member.resolveAttributeName(),
				nonEmptySubgraphNames( fetch.subgraph() ),
				fetchOptions( fetch )
		);
	}

	private static String[] nonEmptySubgraphNames(String[] subgraphNames) {
		if ( subgraphNames.length == 0 ) {
			return subgraphNames;
		}
		final var result = new ArrayList<String>();
		for ( var subgraphName : subgraphNames ) {
			if ( isNotEmpty( subgraphName ) ) {
				result.add( subgraphName );
			}
		}
		return result.toArray( new String[0] );
	}

	private static List<FetchOption> fetchOptions(jakarta.persistence.Fetch fetch) {
		final var options = new ArrayList<FetchOption>();
		options.add( fetch.type() );
		if ( fetch.batchSize() >= 0 ) {
			options.add( new BatchFetch( fetch.batchSize() ) );
		}
		options.add( fetch.cacheStoreMode() );
		options.add( fetch.cacheRetrieveMode() );
		if ( fetch.hints().length > 0 ) {
			options.add( FetchHintOptions.from( fetch.hints() ) );
		}
		return options;
	}

	private static void validateFetchGraphNames(
			List<FetchGraphContribution> fetchGraphContributions,
			NamedEntityGraph[] jpaNamedGraphs,
			String jpaEntityName) {
		if ( fetchGraphContributions.isEmpty() ) {
			return;
		}
		final var jpaGraphNames = new java.util.HashSet<String>();
		for ( var jpaNamedGraph : jpaNamedGraphs ) {
			final String explicitName = jpaNamedGraph.name();
			jpaGraphNames.add( StringHelper.isNotEmpty( explicitName ) ? explicitName : jpaEntityName );
		}
		for ( var fetchGraphContribution : fetchGraphContributions ) {
			if ( !jpaGraphNames.contains( fetchGraphContribution.graphName() ) ) {
				throw new AnnotationException(
						"Attribute '" + fetchGraphContribution.attributeName()
								+ "' is annotated '@Fetch' for an unknown entity graph '"
								+ fetchGraphContribution.graphName() + "'" );
			}
		}
	}
}
