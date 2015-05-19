/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.sql;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
public class StringValueMappingTest extends BaseUnitTestCase {
	private final StringTypeDescriptor stringJavaDescriptor = new StringTypeDescriptor();

	private final VarcharTypeDescriptor varcharSqlDescriptor = new VarcharTypeDescriptor();
	private final ClobTypeDescriptor clobSqlDescriptor = ClobTypeDescriptor.DEFAULT;

	private final WrapperOptions wrapperOptions = new WrapperOptions() {
		public boolean useStreamForLobBinding() {
			return false;
		}

		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}

		public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
			return sqlTypeDescriptor;
		}
	};

	public static final String COLUMN_NAME = "n/a";
	public static final int BIND_POSITION = -1;

	@Test
	public void testNormalVarcharHandling() throws SQLException {
		final ValueExtractor<String> extractor = varcharSqlDescriptor.getExtractor( stringJavaDescriptor );
		final ValueBinder<String> binder = varcharSqlDescriptor.getBinder( stringJavaDescriptor );

		final String fixture = "string value";

		ResultSet resultSet = ResultSetProxy.generateProxy( fixture );
		final String value = extractor.extract( resultSet, COLUMN_NAME, wrapperOptions );
		assertEquals( fixture, value );

		PreparedStatement ps = PreparedStatementProxy.generateProxy( fixture );
		binder.bind( ps, fixture, BIND_POSITION, wrapperOptions );
	}

	@Test
	public void testNullVarcharHandling() throws SQLException {
		final ValueExtractor<String> extractor = varcharSqlDescriptor.getExtractor( stringJavaDescriptor );
		final ValueBinder<String> binder = varcharSqlDescriptor.getBinder( stringJavaDescriptor );

		final String fixture = null;

		ResultSet resultSet = ResultSetProxy.generateProxy( fixture );
		final String value = extractor.extract( resultSet, COLUMN_NAME, wrapperOptions );
		assertEquals( fixture, value );

		PreparedStatement ps = PreparedStatementProxy.generateProxy( fixture );
		binder.bind( ps, fixture, BIND_POSITION, wrapperOptions );
	}

	@Test
	public void testNormalClobHandling() throws SQLException {
		final ValueExtractor<String> extractor = clobSqlDescriptor.getExtractor( stringJavaDescriptor );
		final ValueBinder<String> binder = clobSqlDescriptor.getBinder( stringJavaDescriptor );

		final String fixture = "clob string";
		final Clob clob = new StringClobImpl( fixture );

		ResultSet resultSet = ResultSetProxy.generateProxy( clob );
		final String value = extractor.extract( resultSet, COLUMN_NAME, wrapperOptions );
		assertEquals( fixture, value );

		PreparedStatement ps = PreparedStatementProxy.generateProxy( clob );
		binder.bind( ps, fixture, BIND_POSITION, wrapperOptions );
	}

	@Test
	public void testNullClobHandling() throws SQLException {
		final ValueExtractor<String> extractor = clobSqlDescriptor.getExtractor( stringJavaDescriptor );
		final ValueBinder<String> binder = clobSqlDescriptor.getBinder( stringJavaDescriptor );

		final String fixture = null;
		final Clob clob = null;

		ResultSet resultSet = ResultSetProxy.generateProxy( clob );
		final String value = extractor.extract( resultSet, COLUMN_NAME, wrapperOptions );
		assertNull( value );

		PreparedStatement ps = PreparedStatementProxy.generateProxy( clob );
		binder.bind( ps, fixture, BIND_POSITION, wrapperOptions );
	}
}
