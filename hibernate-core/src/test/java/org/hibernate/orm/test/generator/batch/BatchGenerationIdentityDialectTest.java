package org.hibernate.orm.test.generator.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * On a dialect whose native generation strategy is IDENTITY (e.g. H2, MySQL), persisting outside
 * a transaction delays the insert. A native generator that delegates to identity generation must
 * not take part in batch id generation.
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.NativeGenerationStrategyIsIdentity.class)
@SessionFactory
@DomainModel(annotatedClasses = BatchGenerationIdentityDialectTest.Thing.class)
class BatchGenerationIdentityDialectTest {

	@Test
	void persistOutsideTransaction(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Thing thing = new Thing();
			session.persist( thing );
			session.beginTransaction();
			session.flush();
			session.getTransaction().commit();
			assertThat( thing.id ).isNotNull();
		} );
	}

	@Entity(name = "IdentityThing")
	static class Thing {
		@Id
		@GeneratedValue
		@org.hibernate.annotations.NativeGenerator
		Long id;
		String name;
	}
}
