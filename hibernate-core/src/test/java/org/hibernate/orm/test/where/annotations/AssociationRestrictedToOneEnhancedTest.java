package org.hibernate.orm.test.where.annotations;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BytecodeEnhanced(testEnhancedClasses = { AssociationRestrictedToOneTest.AssociationSqlFkLazy.class,
		AssociationRestrictedToOneTest.AssociationFilterOneJoinLazy.class })
class AssociationRestrictedToOneEnhancedTest extends AssociationRestrictedToOneTest {
	@Test
	void enhancedLoadingAndMutation(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( AssociationSqlFkLazy.class ).getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() )
				.isTrue();
		mappings().forEach( mapping -> {
			try {
				findDistinguishesHiddenAndAbsent( mapping, scope );
				cleanup( scope );
				nativeAssociationFetchRetainsTheStoredKey( mapping, scope );
				cleanup( scope );
				unrelatedUpdatesPreserveHiddenReference( mapping, scope );
				cleanup( scope );
				mergingFilteredNullPreservesHiddenReference( mapping, scope );
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
