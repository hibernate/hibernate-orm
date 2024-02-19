/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * @author Steve Ebersole
 */
public class SimpleHqlInterpretationImpl<R> implements HqlInterpretation<R> {
	private final SqmStatement<R> sqmStatement;
	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;

	public SimpleHqlInterpretationImpl(
			SqmStatement<R> sqmStatement,
			ParameterMetadataImplementor parameterMetadata,
			DomainParameterXref domainParameterXref) {
		this.sqmStatement = sqmStatement;
		this.parameterMetadata = parameterMetadata;
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public SqmStatement<R> getSqmStatement() {
		return sqmStatement;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref.copy();
	}
}
