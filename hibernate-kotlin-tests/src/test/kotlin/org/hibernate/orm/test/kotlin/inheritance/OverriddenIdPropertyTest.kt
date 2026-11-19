package org.hibernate.orm.test.kotlin.inheritance

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import org.hibernate.AnnotationException
import org.hibernate.boot.MetadataSources
import org.hibernate.testing.orm.junit.JiraKey
import org.hibernate.testing.orm.junit.ServiceRegistry
import org.hibernate.testing.orm.junit.ServiceRegistryScope
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * An entity subclass which overrides the identifier property of its root
 * entity has a field of its own with the same name. The field is not
 * annotated `@Id` unless the annotation is repeated on the overriding
 * property, and so the subclass is not treated as redeclaring the identifier.
 */
@JiraKey("HHH-21110")
@ServiceRegistry
class OverriddenIdPropertyTest {

	@Test
	fun overriddenIdProperty(scope: ServiceRegistryScope) {
		assertFalse(Dog::class.java.getDeclaredField("id").isAnnotationPresent(Id::class.java))
		for (subclass in listOf(Dog::class.java, JoinedDog::class.java, UnionDog::class.java)) {
			val metadata = MetadataSources(scope.registry)
				.addAnnotatedClasses(subclass.superclass, subclass)
				.buildMetadata()
			assertTrue(metadata.getEntityBinding(subclass.name)!!.declaredProperties.none { it.name == "id" })
		}
	}

	@Test
	fun annotatedOverriddenIdProperty(scope: ServiceRegistryScope) {
		val sources = MetadataSources(scope.registry)
			.addAnnotatedClasses(Animal::class.java, Cat::class.java)
		val exception = assertThrows(AnnotationException::class.java) { sources.buildMetadata() }
		assertTrue(exception.message!!.contains("'id' of '" + Cat::class.java.name + "'"))
	}

	@Entity(name = "Animal")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	open class Animal(@Id open val id: Long)

	@Entity(name = "Dog")
	class Dog(override val id: Long, val name: String) : Animal(id)

	@Entity(name = "Cat")
	class Cat(@Id override val id: Long, val name: String) : Animal(id)

	@Entity(name = "JoinedAnimal")
	@Inheritance(strategy = InheritanceType.JOINED)
	open class JoinedAnimal(@Id open val id: Long)

	@Entity(name = "JoinedDog")
	class JoinedDog(override val id: Long, val name: String) : JoinedAnimal(id)

	@Entity(name = "UnionAnimal")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	open class UnionAnimal(@Id open val id: Long)

	@Entity(name = "UnionDog")
	class UnionDog(override val id: Long, val name: String) : UnionAnimal(id)
}
