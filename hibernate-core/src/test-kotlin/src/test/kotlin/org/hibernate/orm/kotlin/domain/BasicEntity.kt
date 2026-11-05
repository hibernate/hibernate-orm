package org.hibernate.orm.kotlin.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.Objects

/**
 * Kotlin port of org.hibernate.testing.orm.domain.gambit.BasicEntity,
 */
@Entity
class BasicEntity(
	@Id
	var id: Int?,
	var data: String?
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is BasicEntity) return false
		return id == other.id && data == other.data
	}

	override fun hashCode(): Int {
		return Objects.hash(id, data)
	}
}
