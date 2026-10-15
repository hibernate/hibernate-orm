package org.hibernate.orm.test.onetoone.formula.conversion;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.mapping.ManyToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Ensures writable mixed joins retain logical many-to-one binding and foreign-key writes.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		WritableMixedOneToOneTest.Owner.class,
		WritableMixedOneToOneTest.InsertOnlyOwner.class,
		WritableMixedOneToOneTest.UpdateOnlyOwner.class,
		WritableMixedOneToOneTest.Address.class
})
@SessionFactory
public class WritableMixedOneToOneTest {
	@Test
	public void testWritableJoinsRetainLogicalOneToOne(DomainModelScope scope) {
		for (Class<?> owner : new Class<?>[] { Owner.class, InsertOnlyOwner.class, UpdateOnlyOwner.class }) {
			var value = scope.getEntityBinding(owner).getProperty("address").getValue();
			assertTrue(value instanceof ManyToOne);
			assertTrue(((ManyToOne) value).isLogicalOneToOne());
		}
	}

	@Test
	public void testAssociationAssignmentWritesForeignKey(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Address first = new Address();
			first.person = "First";
			first.type = "HOME";
			session.persist(first);
			Address second = new Address();
			second.person = "Second";
			second.type = "HOME";
			session.persist(second);
			Owner owner = new Owner();
			owner.id = 1L;
			owner.address = first;
			session.persist(owner);
		});
		scope.inTransaction(session -> {
			Owner owner = session.find(Owner.class, 1L);
			assertEquals("First", owner.address.person);
			owner.address = session.find(Address.class, new AddressId("Second", "HOME"));
			session.flush();
			assertEquals("Second", session.createNativeQuery(
					"select addressPerson from WritableOwner where id = 1", String.class).getSingleResult());
			session.clear();
			assertEquals("Second", session.find(Owner.class, 1L).address.person);
			session.createMutationQuery("delete from WritableOwner").executeUpdate();
			session.createMutationQuery("delete from WritableAddress").executeUpdate();
		});
	}

	@Entity(name = "WritableOwner")
	public static class Owner {
		@Id
		Long id;
		@OneToOne(optional = false)
		@JoinColumnsOrFormulas({
				@JoinColumnOrFormula(column = @JoinColumn(name = "addressPerson", referencedColumnName = "personName")),
				@JoinColumnOrFormula(formula = @JoinFormula(value = "'HOME'", referencedColumnName = "addressType"))
		})
		Address address;
	}

	@Entity(name = "InsertOnlyOwner")
	public static class InsertOnlyOwner {
		@Id
		Long id;
		@OneToOne
		@JoinColumnsOrFormulas({
				@JoinColumnOrFormula(column = @JoinColumn(
						name = "addressPerson", referencedColumnName = "personName", updatable = false)),
				@JoinColumnOrFormula(formula = @JoinFormula(value = "'HOME'", referencedColumnName = "addressType"))
		})
		Address address;
	}

	@Entity(name = "UpdateOnlyOwner")
	public static class UpdateOnlyOwner {
		@Id
		Long id;
		@OneToOne
		@JoinColumnsOrFormulas({
				@JoinColumnOrFormula(column = @JoinColumn(
						name = "addressPerson", referencedColumnName = "personName", insertable = false)),
				@JoinColumnOrFormula(formula = @JoinFormula(value = "'HOME'", referencedColumnName = "addressType"))
		})
		Address address;
	}

	@Entity(name = "WritableAddress")
	@IdClass(AddressId.class)
	public static class Address {
		@Id
		@Column(name = "personName")
		String person;
		@Id
		@Column(name = "addressType")
		String type;
	}
}
