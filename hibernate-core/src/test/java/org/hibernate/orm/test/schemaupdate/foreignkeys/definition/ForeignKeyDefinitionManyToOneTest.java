/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.definition;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = H2Dialect.class)
public class ForeignKeyDefinitionManyToOneTest
		extends AbstractForeignKeyDefinitionTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Box.class,
				Thing.class,
		};
	}

	@Entity(name = "Box")
	public static class Box {
		@Id
		public Integer id;

		@ManyToOne
		@JoinColumn(foreignKey = @ForeignKey(name = "thingy", foreignKeyDefinition = "foreign key /* FK */ (thing_id) references Thing"))
		public Thing thing;
	}

	@Entity(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;
	}

	@Override
	protected boolean validate(String fileContent) {
		return fileContent.contains( "/* FK */" );
	}
}
