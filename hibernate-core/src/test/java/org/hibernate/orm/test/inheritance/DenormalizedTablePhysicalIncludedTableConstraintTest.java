/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Paln
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-14234")
@RequiresDialect(
		value = H2Dialect.class,
		comment = "This test relies on 'hibernate.hbm2ddl.halt_on_error', only tested on h2; " +
				"other dialects might be broken due to irrelevant reason (e.g. not supporting 'if exists' while dropping tables)."
)
@DomainModel(
		annotatedClasses = {
				DenormalizedTablePhysicalIncludedTableConstraintTest.SuperClass.class,
				DenormalizedTablePhysicalIncludedTableConstraintTest.SubClass.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @Setting( name = AvailableSettings.HBM2DDL_HALT_ON_ERROR, value = "true"))
public class DenormalizedTablePhysicalIncludedTableConstraintTest {

	@Test
	public void testUniqueConstraintFromSupTableNotAppliedToSubTable() {
		// Unique constraint should be unique in db
		// Without fixing, exception will be thrown when unique constraint in 'supTable' is applied to 'subTable' as well
	}

	@Entity(name = "SuperClass")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table(name = "supTable",
			uniqueConstraints = {
					@UniqueConstraint(name = "UK",
							columnNames = { "colOne", "colTwo" })
			}
	)
	static class SuperClass implements Serializable {

		@Id
		@GeneratedValue
		Long id;

		@Column(name = "colOne")
		Long colOne;

		@Column(name = "colTwo")
		Long colTwo;
	}

	@Entity(name = "SubClass")
	@Table(name = "subTable")
	static class SubClass extends SuperClass {

		@Column(name = "colThree")
		Long colThree;
	}
}
