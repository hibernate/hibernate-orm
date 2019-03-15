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
import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
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
	private final String uid;
	private final NavigablePath navigablePath;
	private final NavigableContainer referencedNavigable;

	private String alias;

	private List<SqmJoin> joins;

	private final UsageDetailsImpl usageDetails = new UsageDetailsImpl( this );

	protected AbstractSqmFrom(
			String uid,
			NavigablePath navigablePath,
			NavigableContainer referencedNavigable,
			String alias) {
		this.uid = uid;
		this.navigablePath = navigablePath;
		this.referencedNavigable = referencedNavigable;
		this.alias = alias;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getUniqueIdentifier() {
		return uid;
	}

	@Override
	public NavigableContainer<?> getReferencedNavigable() {
		return referencedNavigable;
	}

	@Override
	public String getIdentificationVariable() {
		return alias;
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
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return (EntityTypeDescriptor) getUsageDetails().getIntrinsicSubclassIndicator();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		// these calls to
		return getReferencedNavigable().findNavigable( name ).createSqmExpression(
				this,
				creationState
		);
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
