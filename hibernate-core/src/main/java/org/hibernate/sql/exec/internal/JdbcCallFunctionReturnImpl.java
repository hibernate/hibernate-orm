/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class JdbcCallFunctionReturnImpl implements JdbcCallFunctionReturn {
	private final int jdbcTypeCode;

	public JdbcCallFunctionReturnImpl(int jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	public void prepare(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session) {
		if ( jdbcTypeCode == Types.REF_CURSOR ) {
			refCursorSuport( session ).registerRefCursorParameter( callableStatement, 1 );
		}
		else {

		}
	}

	private RefCursorSupport refCursorSuport(SharedSessionContractImplementor session) {
		return session.getFactory().getServiceRegistry().getService( RefCursorSupport.class );
	}

	@Override
	public Object extractOutput(
			CallableStatement callableStatement,
			SharedSessionContractImplementor session) {
		if ( jdbcTypeCode == Types.REF_CURSOR ) {
			// todo : redo JdbcRefCursorExtractor#extractResults to hook in with org.hibernate.sql.result.process
			return new JdbcRefCursorExtractor( null, jdbcTypeCode ).extractResultSet( callableStatement, session );
		}
		else {
			final SqlTypeDescriptor jdbcTypeDescriptor = session.getFactory()
					.getMetamodel()
					.getTypeConfiguration()
					.getSqlTypeDescriptorRegistry().getDescriptor( jdbcTypeCode );
			final JavaTypeDescriptor javaTypeDescriptor = jdbcTypeDescriptor.getJdbcRecommendedJavaTypeMapping(
					session.getFactory().getMetamodel().getTypeConfiguration() );

			try {
				return jdbcTypeDescriptor.getExtractor( javaTypeDescriptor ).extract(
						callableStatement,
						1,
						session
				);
			}
			catch (SQLException e) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "Unable to extract function call result" );
			}
		}
	}
}
