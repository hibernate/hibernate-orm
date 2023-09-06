/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.softdelete;

import java.util.Collection;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.type.BooleanAsBooleanConverter;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * Centralizes the checks about column names, values, etc.
 * to avoid problems across dialects
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		MappingTests.BooleanEntity.class,
		MappingTests.NumericEntity.class,
		MappingTests.TrueFalseEntity.class,
		MappingTests.YesNoEntity.class,
		MappingTests.ReversedYesNoEntity.class,
		MappingTests.CollectionOwner.class,
		MappingTests.CollectionOwned.class
})
@SessionFactory(exportSchema = false)
@SuppressWarnings("unused")
public class MappingTests {
	@Test
	void verifyEntityMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor metamodel = scope.getSessionFactory().getMappingMetamodel();

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( BooleanEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"boolean_entity",
				true
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( NumericEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"numeric_entity",
				1
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( TrueFalseEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"true_false_entity",
				'T'
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( YesNoEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"yes_no_entity",
				'Y'
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( ReversedYesNoEntity.class ).getSoftDeleteMapping(),
				"active",
				"reversed_yes_no_entity",
				'N'
		);
	}

	@Test
	@NotImplementedYet
	void verifyCollectionMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor metamodel = scope.getSessionFactory().getMappingMetamodel();

		MappingVerifier.verifyMapping(
				metamodel.getCollectionDescriptor( CollectionOwner.class.getName() + ".elements" ).getAttributeMapping().getSoftDeleteMapping(),
				"deleted",
				"elements",
				true
		);

		MappingVerifier.verifyMapping(
				metamodel.getCollectionDescriptor( CollectionOwner.class.getName() + ".manyToMany" ).getAttributeMapping().getSoftDeleteMapping(),
				"gone",
				"m2m",
				1
		);
	}

	@Entity(name="BooleanEntity")
	@Table(name="boolean_entity")
	@SoftDelete(converter = BooleanAsBooleanConverter.class)
	public static class BooleanEntity {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="NumericEntity")
	@Table(name="numeric_entity")
	@SoftDelete(converter = NumericBooleanConverter.class)
	public static class NumericEntity {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="TrueFalseEntity")
	@Table(name="true_false_entity")
	@SoftDelete(converter = TrueFalseConverter.class)
	public static class TrueFalseEntity {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="YesNoEntity")
	@Table(name="yes_no_entity")
	@SoftDelete(converter = YesNoConverter.class)
	public static class YesNoEntity {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="ReversedYesNoEntity")
	@Table(name="reversed_yes_no_entity")
	@SoftDelete(columnName = "active", converter = ReverseYesNoConverter.class)
	public static class ReversedYesNoEntity {
		@Id
		private Integer id;
		private String name;
	}

	@Entity
	@Table(name="coll_owned")
	public static class CollectionOwned {
		@Id
		private Integer id;
		@Basic
		private String name;
	}

	@Entity
	@Table(name="coll_owner")
	public static class CollectionOwner {
		@Id
		private Integer id;
		@Basic
		private String name;

		@ElementCollection
		@CollectionTable(name="elements", joinColumns = @JoinColumn(name = "owner_fk"))
		@Column(name="txt")
		@SoftDelete
		private Collection<String> elements;

		@ManyToMany
		@JoinTable(
				name = "m2m",
				joinColumns = @JoinColumn(name = "owner_fk"),
				inverseJoinColumns = @JoinColumn(name="owned_fk")
		)
		@SoftDelete(columnName = "gone", converter = NumericBooleanConverter.class)
		private Collection<CollectionOwned> manyToMany;

		protected CollectionOwner() {
			// for Hibernate use
		}

		public CollectionOwner(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
