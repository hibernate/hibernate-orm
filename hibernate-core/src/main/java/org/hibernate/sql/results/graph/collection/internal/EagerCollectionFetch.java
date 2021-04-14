/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EagerCollectionFetch extends CollectionFetch implements FetchParent {
	private final DomainResult keyContainerResult;
	private final DomainResult keyCollectionResult;

	private final Fetch elementFetch;
	private final Fetch indexFetch;

	private final List<Fetch> fetches;

	private final CollectionInitializerProducer initializerProducer;

	public EagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			TableGroup collectionTableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		super( fetchedPath, fetchedAttribute, fetchParent );

		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		final NavigablePath parentPath = fetchedPath.getParent();
		final TableGroup parentTableGroup = parentPath == null ? null : fromClauseAccess.findTableGroup( parentPath );


		// NOTE :
		// 		`keyContainerResult` = fk target-side
		//		`keyCollectionResult` = fk key-side

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
		if ( parentTableGroup != null ) {
			// join fetch

			// target-side
			keyContainerResult = keyDescriptor.createTargetDomainResult( fetchedPath, parentTableGroup, creationState );

			// referring(key)-side
			keyCollectionResult = keyDescriptor.createKeyDomainResult( fetchedPath, collectionTableGroup, creationState );
		}
		else {
			// select fetch

			// todo (6.0) : we could potentially leverage batch fetching for performance
			keyContainerResult = keyDescriptor.createCollectionFetchDomainResult( fetchedPath, collectionTableGroup, creationState );

			// use null for `keyCollectionResult`... the initializer will see that as trigger to use
			// the assembled container-key value as the collection-key value.
			keyCollectionResult = null;
		}

		fetches = creationState.visitFetches( this );
		if ( fetchedAttribute.getIndexDescriptor() != null ) {
			assert fetches.size() == 2;
			indexFetch = fetches.get( 0 );
			elementFetch = fetches.get( 1 );
		}
		else {
			if ( !fetches.isEmpty() ) { // might be empty due to fetch depth limit
				assert fetches.size() == 1;
				indexFetch = null;
				elementFetch = fetches.get( 0 );
			}
			else {
				indexFetch = null;
				elementFetch = null;
			}
		}

		final CollectionSemantics collectionSemantics = getFetchedMapping().getCollectionDescriptor().getCollectionSemantics();
		initializerProducer = collectionSemantics.createInitializerProducer(
				fetchedPath,
				fetchedAttribute,
				fetchParent,
				true,
				null,
				// todo (6.0) : we need to propagate these lock modes
				LockMode.READ,
				indexFetch,
				elementFetch,
				creationState
		);
	}

	@Override
	public DomainResultAssembler createAssembler(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		final CollectionInitializer initializer = (CollectionInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> {
					final DomainResultAssembler keyContainerAssembler = keyContainerResult.createResultAssembler( creationState );

					final DomainResultAssembler keyCollectionAssembler;
					if ( keyCollectionResult == null ) {
						keyCollectionAssembler = null;
					}
					else {
						keyCollectionAssembler = keyCollectionResult.createResultAssembler( creationState );
					}

					return initializerProducer.produceInitializer(
							getNavigablePath(),
							getFetchedMapping(),
							parentAccess,
							null,
							keyContainerAssembler,
							keyCollectionAssembler,
							creationState
					);
				}
		);

		return new EagerCollectionAssembler( getFetchedMapping(), initializer );
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
	public FetchableContainer getReferencedMappingContainer() {
		return getFetchedMapping();
	}

	@Override
	public PluralAttributeMapping getReferencedMappingType() {
		return getFetchedMapping();
	}

	@Override
	public List<Fetch> getFetches() {
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
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return getFetchedMapping().getJavaTypeDescriptor();
	}
}
