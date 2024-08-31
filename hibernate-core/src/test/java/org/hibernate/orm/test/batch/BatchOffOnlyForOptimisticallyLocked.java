package org.hibernate.orm.test.batch;

import java.io.Serializable;
import java.util.List;

import org.hibernate.cfg.BatchSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import jakarta.persistence.Version;

@Jpa(
		annotatedClasses = BatchOffOnlyForOptimisticallyLocked.Something.class,
		properties = {
				@Setting(name = BatchSettings.STATEMENT_BATCH_SIZE, value = "10"),
				@Setting(name = BatchSettings.BATCH_VERSIONED_DATA, value = "false")
		}
)
@JiraKey( "HHH-18621" )
public class BatchOffOnlyForOptimisticallyLocked {
	@Test
	public void testMultiUpdateOfConcurrentlyModified(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Something( "First" ) );
			em.persist( new Something( "Second" ) );
		} );

		final RollbackException ex = Assertions.assertThrows( RollbackException.class, () -> {
			scope.inTransaction( em -> {
				final List<Something> subjects = em.createQuery( "select s from Something s", Something.class )
						.getResultList();
				scope.inTransaction(
						competitorEm -> competitorEm.find( Something.class, subjects.get( 0 ).id ).name = "Outrun"
				);
				for ( Something something : subjects ) {
					something.name += " modified";
				}
			} );
		} );
		Assertions.assertInstanceOf( OptimisticLockException.class, ex.getCause(), "The cause of rollback" );
		Assertions.assertNotNull( ( (OptimisticLockException) ex.getCause() ).getEntity(), "OLE references an entity" );
	}

	//Has to be Serializable, otherwise it is not deemed safe to include in the OLE.
	@Entity(name = "Something")
	public static class Something implements Serializable {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;
		public String name;
		@Version
		public long version;

		public Something() {
		}

		public Something(String name) {
			this.name = name;
		}
	}
}
