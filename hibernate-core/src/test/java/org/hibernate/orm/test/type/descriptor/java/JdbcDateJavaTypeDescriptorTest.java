package org.hibernate.orm.test.type.descriptor.java;

import java.sql.Date;

import org.hibernate.type.descriptor.java.JdbcDateJavaTypeDescriptor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@BaseUnitTest
public class JdbcDateJavaTypeDescriptorTest {

	@Test
	public void testToString() {
		final JdbcDateJavaTypeDescriptor javaTypeDescriptor = JdbcDateJavaTypeDescriptor.INSTANCE;

		final String utilDate = javaTypeDescriptor.toString( new java.util.Date( 0 ) );
		assertThat( utilDate ).isEqualTo( "01 January 1970" );

		final String sqlDate = javaTypeDescriptor.toString( new java.sql.Date( 0 ) );
		assertThat( sqlDate ).isEqualTo( "01 January 1970" );
	}

	@Test
	public void testIsInstance() {
		final JdbcDateJavaTypeDescriptor javaTypeDescriptor = JdbcDateJavaTypeDescriptor.INSTANCE;

		javaTypeDescriptor.isInstance( new java.sql.Date( 0 ) );
		javaTypeDescriptor.isInstance( new java.util.Date( 0 ) );
	}

	@Test
	public void testWrap() {
		final JdbcDateJavaTypeDescriptor javaTypeDescriptor = JdbcDateJavaTypeDescriptor.INSTANCE;

		final Date sqlDate = new Date( 0 );
		final java.util.Date utilDate = new java.util.Date( 0 );

		javaTypeDescriptor.isInstance( sqlDate );
		javaTypeDescriptor.isInstance( utilDate );
	}
}
