package org.hibernate.orm.test.locking;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.H2Dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies binary-key eligibility before the shared locking fixture is bootstrapped.
///
/// @author Steve Ebersole
class CompositeIdentifierDomainModelTest {
	@Test
	void omitsBothBinaryKeyMappingsForInformix() {
		assertThat( new CompositeIdentifierFollowOnLockingTest.BinaryIdentifierModel( InformixDialect.class )
				.getAnnotatedClasses() ).isEmpty();
	}

	@Test
	void retainsBothBinaryKeyMappingsForSupportingDialects() {
		assertThat( new CompositeIdentifierFollowOnLockingTest.BinaryIdentifierModel( H2Dialect.class )
				.getAnnotatedClasses() ).containsExactly(
				CompositeIdentifierFollowOnLockingTest.BinaryItem.class,
				CompositeIdentifierFollowOnLockingTest.TypedItem.class );
	}
}
