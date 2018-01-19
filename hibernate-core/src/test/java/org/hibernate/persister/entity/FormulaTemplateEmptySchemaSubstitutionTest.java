package org.hibernate.persister.entity;

import java.util.Properties;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Persister;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mykhaylo Gnylorybov
 */
@RequiresDialect(H2Dialect.class)
public class FormulaTemplateEmptySchemaSubstitutionTest extends BaseCoreFunctionalTestCase {

	private static final String SCHEMA_PLACEHOLDER = "h-schema";

	@Test
	public void replaceTemplateWithEmptyDefaultSchema() {
		final String className = FormulaTemplateEmptySchemaSubstitutionTest.FooBar.class.getName();
		final AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory().getEntityPersister(
				className );
		final String formula = persister.getSubclassFormulaTemplateClosure()[0];

		doInHibernate( this::sessionFactory, session -> {
			FormulaTemplateEmptySchemaSubstitutionTest.Foo foo = new FormulaTemplateEmptySchemaSubstitutionTest.Foo();
			foo.id = 1;
			foo.name = "fooName";
			session.persist( foo );
			FormulaTemplateEmptySchemaSubstitutionTest.Bar bar = new FormulaTemplateEmptySchemaSubstitutionTest.Bar();
			bar.id = 2;
			bar.name = "barName";
			session.persist( bar );
			FormulaTemplateEmptySchemaSubstitutionTest.FooBar fooBar =
					new FormulaTemplateEmptySchemaSubstitutionTest.FooBar();
			fooBar.id = 3;
			fooBar.bar = bar;
			fooBar.foo = foo;
			session.persist( fooBar );
		} );

		doInHibernate( this::sessionFactory, session -> {
			FooBar entity = session.find( FooBar.class, 3 );
			assertTrue( "Invalid result of formula expression: ", entity.isValid );
		} );

		assertTrue( "Formula should not contain {} characters", formula.matches( "^[^{}]+$" ) );
		assertFalse( "Formula should not contain hibernate placeholder", formula.contains( SCHEMA_PLACEHOLDER ) );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				FormulaTemplateEmptySchemaSubstitutionTest.FooBar.class,
				FormulaTemplateEmptySchemaSubstitutionTest.Bar.class,
				FormulaTemplateEmptySchemaSubstitutionTest.Foo.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		final Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		properties.remove( "hibernate.default_schema" );
		configuration.setProperties( properties );
	}

	@Entity(name = "FOOBAR")
	@Table(name = "FOOBAR")
	@Persister(impl = SingleTableEntityPersister.class)

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
