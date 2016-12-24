/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.sql.SQLException;

import org.hibernate.sql.convert.results.internal.ReturnEntityImpl;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;

/**
 * @author Steve Ebersole
 */
public class ReturnAssemblerEntity implements ReturnAssembler {
	private final ReturnEntityImpl returnEntity;

	public ReturnAssemblerEntity(ReturnEntityImpl returnEntity) {
		this.returnEntity = returnEntity;
	}

	@Override
	public Class getReturnedJavaType() {
		return returnEntity.getReturnedJavaType();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		return returnEntity.getInitializer().getEntityInstance();
	}
}
