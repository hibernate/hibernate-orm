/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.service.ServiceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ResultSetWrapperProxyTest {

	private ResultSet resultSet;

	private ResultSet resultSetProxy;

	@Before
	public void initialize() throws SQLException {
		ServiceRegistry serviceRegistry = Mockito.mock( ServiceRegistry.class );
		when( serviceRegistry.getService( eq( ClassLoaderService.class ) ) ).thenReturn( new ClassLoaderServiceImpl() );

		ColumnNameCache columnNameCache = new ColumnNameCache( 2 );

		resultSet = Mockito.mock( ResultSet.class );
		when( resultSet.findColumn( eq( "myColumn" ) ) ).thenReturn( 1 );

		resultSetProxy = ResultSetWrapperProxy.generateProxy( resultSet, columnNameCache, serviceRegistry );
	}

	@Test
	public void testRedirectedGetMethod() throws SQLException {
		resultSetProxy.getBigDecimal( "myColumn" );

		verify( resultSet, times( 1 ) ).getBigDecimal( 1 );
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRedirectedGetMethodWithAdditionalParameters() throws SQLException {
		resultSetProxy.getBigDecimal( "myColumn", 8 );

		verify( resultSet, times( 1 ) ).getBigDecimal( 1, 8 );
	}

	@Test
	public void testRedirectedUpdateMethod() throws SQLException {
		resultSetProxy.updateInt( "myColumn", 19 );

		verify( resultSet, times( 1 ) ).updateInt( 1, 19 );
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testIntMethods() throws SQLException {
		resultSetProxy.getBigDecimal( 3 );
		verify( resultSet, times( 1 ) ).getBigDecimal( 3 );

		resultSetProxy.getBigDecimal( 13, 8 );
		verify( resultSet, times( 1 ) ).getBigDecimal( 13, 8 );

		resultSetProxy.updateInt( 23, 19 );
		verify( resultSet, times( 1 ) ).updateInt( 23, 19 );
	}

	@Test
	public void testStandardMethod() throws SQLException {
		resultSetProxy.getFetchSize();

		verify( resultSet, times( 1 ) ).getFetchSize();
	}
}
