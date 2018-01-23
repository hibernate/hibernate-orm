package org.hibernate.persister.entity;

import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RequiresDialect(H2Dialect.class)
public class FormulaTemplateSchemaSubstitutionTest extends BaseCoreFunctionalTestCase {

	private static final String CUSTOM_SCHEMA = "CUSTOM_SCHEMA";
	private static final int EXPECTED_SUBSTITUTIONS = 4;

	@Test
	public void replaceTemplateWithDefaultSchema() {
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
			FormulaTemplateEmptySchemaSubstitutionTest.FooBar entity = session.find(
					FormulaTemplateEmptySchemaSubstitutionTest.FooBar.class, 3 );
			assertTrue( "Invalid result of formula expression: ", entity.isValid );
		} );

		assertEquals( "Formula should not contain {} characters",
					  EXPECTED_SUBSTITUTIONS, formula.split( CUSTOM_SCHEMA + ".", -1 ).length - 1
		);
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
		properties.put( "hibernate.default_schema", CUSTOM_SCHEMA );
		configuration.addProperties( properties );
	}

	@Override
	protected String createSecondSchema() {
		return CUSTOM_SCHEMA;
	}
}
