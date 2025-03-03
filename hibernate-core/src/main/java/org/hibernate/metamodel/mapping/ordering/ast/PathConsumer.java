/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.query.hql.internal.BasicDotIdentifierConsumer;

import org.jboss.logging.Logger;

/**
 * Represents the translation of an individual part of a path in `@OrderBy` translation
 *
 * Similar in purpose to {@link org.hibernate.query.hql.spi.DotIdentifierConsumer}, but for `@OrderBy` translation
 *
 * @author Steve Ebersole
 */
public class PathConsumer {
	private static final Logger log = Logger.getLogger( BasicDotIdentifierConsumer.class );

	private final TranslationContext translationContext;

	private final SequencePart rootSequencePart;

	private final StringBuilder pathSoFar = new StringBuilder();
	private SequencePart currentPart;

	public PathConsumer(
			PluralAttributeMapping pluralAttributeMapping,
			TranslationContext translationContext) {
		this.translationContext = translationContext;

		this.rootSequencePart = new RootSequencePart( pluralAttributeMapping );
	}

	public SequencePart getConsumedPart() {
		return currentPart;
	}

	public void consumeIdentifier(
			String unquotedIdentifier,
			String identifier, boolean isBase,
			boolean isTerminal) {
		if ( isBase ) {
			// each time we start a new sequence we need to reset our state
			reset();
		}

		if ( pathSoFar.length() != 0 ) {
			pathSoFar.append( '.' );
		}
		pathSoFar.append( unquotedIdentifier );

		log.tracef(
				"BasicDotIdentifierHandler#consumeIdentifier( %s, %s, %s ) - %s",
				unquotedIdentifier,
				isBase,
				isTerminal,
				pathSoFar
		);

		currentPart = currentPart.resolvePathPart( unquotedIdentifier, identifier, isTerminal, translationContext );
	}

	private void reset() {
		pathSoFar.setLength( 0 );
		currentPart = rootSequencePart;
	}
}
