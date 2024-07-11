/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.BitSet;

import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public class EagerCollectionFetch extends CollectionFetch {
	private final PluralTableGroup collectionTableGroup;
	private final @Nullable DomainResult<?> collectionKeyResult;
	private final DomainResult<?> collectionValueKeyResult;

	private final Fetch elementFetch;
	private final Fetch indexFetch;

	private final ImmutableFetchList fetches;

	private final CollectionInitializerProducer initializerProducer;

	public EagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			TableGroup collectionTableGroup,
			boolean needsCollectionKeyResult,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		super( fetchedPath, fetchedAttribute, fetchParent );
		this.collectionTableGroup = (PluralTableGroup) collectionTableGroup;

		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		final NavigablePath parentPath = fetchedPath.getParent();
		final TableGroup parentTableGroup = fromClauseAccess.findTableGroup( parentPath );


		// NOTE :
		// 		`collectionKeyResult` = fk target-side
		//		`collectionValueKeyResult` = fk key-side

		// 3 cases:
		//
		//		#1 - one-to-many : Teacher#students
		//			teacher( id ), student( id, teacher_fk )
		//
		//			student.teacher_fk -> teacher.id
		//
		//			referring : student.teacher_fk
		//			target : teacher.id
		//
		//		#2 - many-to-many : Teacher#skills
		//			teacher( id ), skill( id ), teacher_skill( teacher_fk, skill_id )
		//
		//			teacher_skill.skill_id -> skill.id
		//			teacher_skill.teacher_fk -> teacher.id
		//
		//			referring : teacher_skill.teacher_fk
		//			target : teacher.id
		//
		//		#3 - element-collection : Teacher#nickNames
		//			teacher( id ), teacher_nicks( teacher_fk, nick )
		//
		//			teacher_nicks.teacher_fk -> teacher.id
		//
		//			referring : teacher_nicks.teacher_fk
		//			target : teacher.id

		final ForeignKeyDescriptor keyDescriptor = fetchedAttribute.getKeyDescriptor();
		// The collection key must be fetched from the side of the declaring type of the attribute
		// So that this is guaranteed to be not-null
		if ( needsCollectionKeyResult ) {
			collectionKeyResult = keyDescriptor.createTargetDomainResult(
					fetchedPath,
					parentTableGroup,
					fetchParent,
					creationState
			);
		}
		else {
			collectionKeyResult = null;
		}
		// The collection is always the target side
		collectionValueKeyResult = keyDescriptor.createKeyDomainResult(
				fetchedPath,
				collectionTableGroup,
				ForeignKeyDescriptor.Nature.TARGET,
				fetchParent,
				creationState
		);

		fetches = creationState.visitFetches( this );
		if ( fetchedAttribute.getIndexDescriptor() != null ) {
			assert fetches.size() == 2;
			indexFetch = fetches.get( fetchedAttribute.getIndexDescriptor() );
			elementFetch = fetches.get( fetchedAttribute.getElementDescriptor() );
		}
		else {
			if ( !fetches.isEmpty() ) { // might be empty due to fetch depth limit
				assert fetches.size() == 1;
				indexFetch = null;
				elementFetch = fetches.get( fetchedAttribute.getElementDescriptor() );
			}
			else {
				indexFetch = null;
				elementFetch = null;
			}
		}

		final CollectionSemantics<?, ?> collectionSemantics = getFetchedMapping().getCollectionDescriptor().getCollectionSemantics();
		initializerProducer = collectionSemantics.createInitializerProducer(
				fetchedPath,
				fetchedAttribute,
				fetchParent,
				true,
				null,
				indexFetch,
				elementFetch,
				creationState
		);
	}

	@Override
	public NavigablePath resolveNavigablePath(Fetchable fetchable) {
		// Only CollectionPart is possible here
		return getNavigablePath().append( fetchable.getFetchableName() );
	}

	@Override
	public CollectionInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return initializerProducer.produceInitializer(
				getNavigablePath(),
				getFetchedMapping(),
				parent,
				null,
				collectionKeyResult,
				collectionValueKeyResult,
				false,
				creationState
		);
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	@Override
	public ImmutableFetchList getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		if ( CollectionPart.Nature.ELEMENT.getName().equals( fetchable.getFetchableName() ) ) {
			return elementFetch;
		}
		else if ( CollectionPart.Nature.INDEX.getName().equals( fetchable.getFetchableName() ) ) {
			return indexFetch;
		}
		else {
			throw new IllegalArgumentException(
					"Unknown fetchable [" + getFetchedMapping().getCollectionDescriptor().getRole() +
							" -> " + fetchable.getFetchableName() + "]"
			);
		}
	}

	@Override
	public boolean hasJoinFetches() {
		// This is already a fetch, so this line should actually never be hit
		return true;
	}

	@Override
	public boolean containsCollectionFetches() {
		// This is already a collection fetch, so this line should actually never be hit
		return true;
	}

	@Override
	public int getCollectionFetchesCount() {
		return 1 + super.getCollectionFetchesCount();
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getFetchedMapping().getJavaType();
	}

	@Override
	public FetchParent asFetchParent() {
		return this;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( collectionKeyResult != null ) {
			collectionKeyResult.collectValueIndexesToCache( valueIndexes );
		}
		if ( !getFetchedMapping().getCollectionDescriptor().useShallowQueryCacheLayout() ) {
			collectionValueKeyResult.collectValueIndexesToCache( valueIndexes );
			for ( Fetch fetch : fetches ) {
				fetch.collectValueIndexesToCache( valueIndexes );
			}
		}
	}
}
