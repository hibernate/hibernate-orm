package org.hibernate.orm.test.type.descriptor.java;

import java.sql.Date;

import org.hibernate.type.descriptor.java.JdbcDateJavaType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@BaseUnitTest
public class JdbcDateJavaTypeDescriptorTest {

	@Test
	public void testToString() {
		final JdbcDateJavaType javaType = JdbcDateJavaType.INSTANCE;

		final String utilDate = javaType.toString( new java.util.Date( 0 ) );
		assertThat( utilDate ).isEqualTo( "01 January 1970" );

		final String sqlDate = javaType.toString( new java.sql.Date( 0 ) );
		assertThat( sqlDate ).isEqualTo( "01 January 1970" );
	}

	@Test
	public void testIsInstance() {
		final JdbcDateJavaType javaType = JdbcDateJavaType.INSTANCE;

		javaType.isInstance( new java.sql.Date( 0 ) );
		javaType.isInstance( new java.util.Date( 0 ) );
	}

	@Test
	public void testWrap() {
		final JdbcDateJavaType javaType = JdbcDateJavaType.INSTANCE;

		final Date sqlDate = new Date( 0 );
		final java.util.Date wrappedSqlDate = javaType.wrap( sqlDate, null );
		assertThat( wrappedSqlDate ).isSameAs( sqlDate );

		final java.util.Date utilDate = new java.util.Date( 0 );
		final java.util.Date wrappedUtilDate = javaType.wrap( utilDate, null );
		assertThat( wrappedUtilDate ).isInstanceOf( java.sql.Date.class );
	}
}
