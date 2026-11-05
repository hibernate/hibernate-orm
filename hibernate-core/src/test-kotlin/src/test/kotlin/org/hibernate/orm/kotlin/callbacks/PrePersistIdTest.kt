package org.hibernate.orm.kotlin.callbacks

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Transient
import org.hibernate.testing.orm.junit.DomainModel
import org.hibernate.testing.orm.junit.SessionFactory
import org.hibernate.testing.orm.junit.SessionFactoryScope
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

@SessionFactory
@DomainModel(annotatedClasses = [PrePersistIdTest.GeneratedIdInCallback::class])
class PrePersistIdTest {

	@Test
	fun test(scope: SessionFactoryScope) {
		scope.inTransaction { session ->
			val entity = GeneratedIdInCallback()
			session.persist(entity)
			assertTrue(entity.success)
			assertNotNull(entity.uuid)
		}
	}

	@Entity(name = "GeneratedIdInCallback")
	class GeneratedIdInCallback {
		@Transient
		var success: Boolean = false

		@Id
		@GeneratedValue
		var uuid: UUID? = null

		@PrePersist
		fun checkId() {
			success = uuid != null
			assertNotNull(uuid)
		}
	}
}
