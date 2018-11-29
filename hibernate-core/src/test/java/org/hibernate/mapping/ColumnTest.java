package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


public class ColumnTest {

	private Column underTest = new Column();

	@Test
	public void testGetAliasWithTableMaxLength(){

		// choosing this as it defines a maxAliasLength of 30
		Dialect dialect = new SybaseASE15Dialect();
		assertThat(dialect.getMaxAliasLength(), is(equalTo(30)));
		Table table = new Table();
		// used in the alias generation:
		underTest.setName("AN_EXTREMELY_LONG_COLUM_NAME"); // (28 chars)
		underTest.uniqueInteger = 10;
		table.setUniqueInteger(10);
		String result = underTest.getAlias(dialect, table);
		assertThat(result.length(), is(lessThanOrEqualTo(dialect.getMaxAliasLength())));
	}

}
