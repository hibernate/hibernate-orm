/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BytecodeEnhanced(testEnhancedClasses = { RestrictedToOneTest.SqlFk.class, RestrictedToOneTest.FilterJoinLazy.class })
class RestrictedToOneEnhancedTest extends RestrictedToOneTest {
	@Test
	void enhancedMappings(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( SqlFk.class ).getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ).isTrue();
	}

	@Test
	void enhancedFindDistinguishesHiddenAndAbsent(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::findDistinguishesHiddenAndAbsent );
	}

	@Test
	void enhancedQueriesAndFetchJoinsAgree(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::queriesAndFetchJoinsAgree );
	}

	@Test
	void enhancedEntityGraphsAgree(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::entityGraphsAgree );
	}

	@Test
	void enhancedUnrelatedUpdatesPreserveHiddenReference(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::unrelatedUpdatesPreserveHiddenReference );
	}

	@Test
	void enhancedAssigningNullToHiddenReferencePreservesIt(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::assigningNullToHiddenReferencePreservesIt );
	}

	@Test
	void enhancedSerializedSessionPreservesHiddenReference(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::serializedSessionPreservesHiddenReference );
	}

	@Test
	void enhancedMergingFilteredNullPreservesHiddenReference(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::mergingFilteredNullPreservesHiddenReference );
	}

	@Test
	void enhancedReplacementThenClearIsNotSuppressed(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::replacementThenClearIsNotSuppressed );
	}

	@Test
	void enhancedVisibleReferenceCanBeClearedByMerge(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::visibleReferenceCanBeClearedByMerge );
	}

	@Test
	void enhancedAbsentReferenceCanBeAssigned(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::absentReferenceCanBeAssigned );
	}

	@Test
	void enhancedTargetIdNavigationHonorsRestriction(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::targetIdNavigationHonorsRestriction );
	}

	@Test
	void enhancedMergeDoesNotNeedDetachedMetadata(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::mergeDoesNotNeedDetachedMetadata );
	}

	@Test
	void enhancedUnrelatedUpdateDoesNotCreateAbsentAssociation(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::unrelatedUpdateDoesNotCreateAbsentAssociation );
	}

	@Test
	void enhancedRefreshPreservesHiddenReference(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::refreshPreservesHiddenReference );
	}

	@Test
	void enhancedReadOnlyToModifiablePreservesHiddenReference(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::readOnlyToModifiablePreservesHiddenReference );
	}

	@Test
	void enhancedOnlyVisibleRemovalDeletesAnOrphan(SessionFactoryScope scope) {
		checkMappings( orphanMappings(), scope, this::onlyVisibleRemovalDeletesAnOrphan );
	}

	@Test
	void enhancedUpdatesInOneFlushKeepEachRowsNullness(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::updatesInOneFlushKeepEachRowsNullness );
	}

	@Test
	void enhancedNativeAssociationFetchRetainsTheStoredKey(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::nativeAssociationFetchRetainsTheStoredKey );
	}

	@Test
	void enhancedDeletingOwnerRemovesHiddenJoinRow(SessionFactoryScope scope) {
		checkMappings( mappings(), scope, this::deletingOwnerRemovesHiddenJoinRow );
	}

	@Test
	void enhancedChangingFilterParametersDoesNotReuseThePreviousValue(SessionFactoryScope scope) {
		checkMappings( filterMappings(), scope, this::changingFilterParametersDoesNotReuseThePreviousValue );
	}

	@Test
	void enhancedRefreshCanRevealAReferenceWhichCanThenBeCleared(SessionFactoryScope scope) {
		checkMappings( filterMappings(), scope, this::refreshCanRevealAReferenceWhichCanThenBeCleared );
	}

	private void checkMappings(
			Stream<Mapping> mappings,
			SessionFactoryScope scope,
			BiConsumer<Mapping, SessionFactoryScope> test) {
		mappings.forEach( mapping -> {
			try {
				test.accept( mapping, scope );
			}
			catch (RuntimeException | AssertionError failure) {
				throw new AssertionError( mapping.toString(), failure );
			}
			finally {
				cleanup( scope );
			}
		} );
	}
}
