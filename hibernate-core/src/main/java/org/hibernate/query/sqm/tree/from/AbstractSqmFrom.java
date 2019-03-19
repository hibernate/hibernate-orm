/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;

/**
 * Convenience base class for SqmFrom implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFrom implements SqmFrom {
	private final NavigablePath navigablePath;
	private final NavigableContainer referencedNavigable;

	private String alias;

	private List<SqmJoin> joins;

	private final UsageDetailsImpl usageDetails = new UsageDetailsImpl( this );

	protected AbstractSqmFrom(
			NavigablePath navigablePath,
			NavigableContainer referencedNavigable,
			String alias) {
		this.navigablePath = navigablePath;
		this.referencedNavigable = referencedNavigable;
		this.alias = alias;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainer<?> getReferencedNavigable() {
		return referencedNavigable;
	}

	@Override
	public String getExplicitAlias() {
		return alias;
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		this.alias = explicitAlias;
	}

	@Override
	public UsageDetails getUsageDetails() {
		return usageDetails;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		// these calls to
		final Navigable navigable = getReferencedNavigable().findNavigable( name );
		if ( navigable == null ) {
			throw new QueryException(
					String.format(
							Locale.ROOT,
							"could not resolve property: %s of: %s",
							name,
							getReferencedNavigable().getNavigableName()
					)
			);
		}
		return navigable.createSqmExpression( this, creationState );
	}

	@Override
	public boolean hasJoins() {
		return ! (joins == null || joins.isEmpty() );
	}

	@Override
	public List<SqmJoin> getJoins() {
		return joins == null ? Collections.emptyList() : Collections.unmodifiableList( joins );
	}

	@Override
	public void addJoin(SqmJoin join) {
		if ( joins == null ) {
			joins = new ArrayList<>();
		}
		joins.add( join );
	}

	@Override
	public void visitJoins(Consumer<SqmJoin> consumer) {
		if ( joins != null ) {
			joins.forEach( consumer );
		}
	}
}
