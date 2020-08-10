/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.List;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityAssembler;
import org.hibernate.sql.results.graph.entity.internal.EntityResultInitializer;

/**
 * @author Steve Ebersole
 */
public class EntityResultImpl implements EntityResult {
	private final NavigablePath navigablePath;
	private final EntityValuedModelPart entityValuedModelPart;

	private final DomainResult identifierResult;
	private final BasicResult discriminatorResult;
	private final List<Fetch> fetches;

	private final String resultAlias;
	private final LockMode lockMode;

	public EntityResultImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			String resultAlias,
			LockMode lockMode,
			DomainResult identifierResult,
			BasicResult discriminatorResult,
			Function<FetchParent,List<Fetch>> fetchesProducer) {
		this.navigablePath = navigablePath;
		this.entityValuedModelPart = entityValuedModelPart;
		this.resultAlias = resultAlias;
		this.lockMode = lockMode;
		this.identifierResult = identifierResult;
		this.discriminatorResult = discriminatorResult;

		this.fetches = fetchesProducer.apply( this );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityValuedModelPart getReferencedMappingType() {
		return entityValuedModelPart;
	}

	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return entityValuedModelPart;
	}

	@Override
	public String getResultVariable() {
		return resultAlias;
	}

	@Override
	public List<Fetch> getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		for ( int i = 0; i < fetches.size(); i++ ) {
			if ( fetches.get( i ).getFetchedMapping() == fetchable ) {
				return fetches.get( i );
			}
		}

		return null;
	}

	@Override
	public DomainResultAssembler createResultAssembler(AssemblerCreationState creationState) {
		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> new EntityResultInitializer(
						this,
						getNavigablePath(),
						lockMode,
						identifierResult,
						discriminatorResult,
						null,
						null,
						creationState
				)
		);

		return new EntityAssembler( getResultJavaTypeDescriptor(), initializer );
	}
}
