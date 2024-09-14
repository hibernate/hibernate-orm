/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
		MappingTests.ReversedYesNoEntity.class
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

	@Entity(name="BooleanEntity")
	@Table(name="boolean_entity")
	@SoftDelete()
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
	@SoftDelete(converter = YesNoConverter.class, strategy = SoftDeleteType.ACTIVE)
	public static class ReversedYesNoEntity {
		@Id
		private Integer id;
		private String name;
	}
}
