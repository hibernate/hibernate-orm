package org.hibernate.orm.test.type.descriptor.java;

import java.util.Date;

import org.hibernate.type.descriptor.java.JdbcDateJavaTypeDescriptor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BaseUnitTest
public class JdbcDateJavaTypeDescriptorTest {

	@Test
	public void testToString() {
		final JdbcDateJavaTypeDescriptor javaTypeDescriptor = JdbcDateJavaTypeDescriptor.INSTANCE;
		final String actual = javaTypeDescriptor.toString( new Date( 0 ) );
		assertEquals( "01 January 1970", actual );
	}
}
