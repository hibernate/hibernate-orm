/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.persister.entity.AbstractEntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Mykhaylo Gnylorybov
 */
@DomainModel(annotatedClasses = {
		AbstractSchemaSubstitutionFormulaTest.FooBar.class,
		AbstractSchemaSubstitutionFormulaTest.Bar.class,
		AbstractSchemaSubstitutionFormulaTest.Foo.class
})
@SessionFactory
public abstract class AbstractSchemaSubstitutionFormulaTest {

	protected static final String SCHEMA_PLACEHOLDER = "h-schema";

	@Test
	public void test(SessionFactoryScope scope) {
		final String className = FooBar.class.getName();
		final AbstractEntityPersister persister = (AbstractEntityPersister) scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( className );
		final String formula = persister.getSubclassPropertyFormulaTemplateClosure()[persister.getPropertyIndex( "isValid" )][0];
		validate( formula );

		scope.inTransaction( session -> {
			Foo foo = new Foo();
			foo.id = 1;
			foo.name = "fooName";
			session.persist( foo );

			Bar bar = new Bar();
			bar.id = 2;
			bar.name = "barName";
			session.persist( bar );

			FooBar fooBar = new FooBar();
			fooBar.id = 3;
			fooBar.bar = bar;
			fooBar.foo = foo;

			session.persist( fooBar );
		} );

		scope.inTransaction( session -> {
			FooBar entity = session.find( FooBar.class, 3 );
			assertTrue( "Invalid result of formula expression: ", entity.isValid );
		} );


	}

	abstract void validate(String formula);

	@Entity(name = "FOOBAR")
	@Table(name = "FOOBAR")
	public static class FooBar {

		@Id
		@Column(name = "ID")
		public Integer id;

		@ManyToOne
		@JoinColumn(name = "FOO_ID")
		public Foo foo;

		@ManyToOne
		@JoinColumn(name = "BAR_ID")
		public Bar bar;

		@Formula("CASE WHEN (\n"
				+ "    EXISTS (\n"
				+ "        SELECT *\n"
				+ "            FROM {h-schema}Foo foo\n"
				+ "            JOIN {h-schema}FooBar fooBar\n"
				+ "            ON foo.ID = fooBar.FOO_ID"
				+ "            WHERE foo.name IS NOT NULL\n"
				+ "         )\n"
				+ "    AND\n"
				+ "    EXISTS (\n"
				+ "        SELECT *\n"
				+ "            FROM {h-schema}Bar bar\n"
				+ "            JOIN {h-schema}FooBar fooBar\n"
				+ "            ON bar.ID = fooBar.BAR_ID\n"
				+ "            WHERE bar.name IS NOT NULL\n"
				+ "        ))\n"
				+ "    THEN 1\n"
				+ "    ELSE 0\n"
				+ "END")
		public Boolean isValid;
	}

	@Entity(name = "FOO")
	@Table(name = "FOO")
	public static class Foo {

		@Id
		@Column(name = "ID")
		public Integer id;

		@Column(name = "name")
		public String name;
	}

	@Entity(name = "BAR")
	@Table(name = "BAR")
	public static class Bar {

		@Id
		@Column(name = "ID")
		public Integer id;

		@Column(name = "name")
		public String name;
	}
}
