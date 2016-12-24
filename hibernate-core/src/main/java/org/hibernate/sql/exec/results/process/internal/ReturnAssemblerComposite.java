/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.convert.results.internal.ReturnCompositeImpl;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class ReturnAssemblerComposite implements ReturnAssembler {
	private final ReturnCompositeImpl returnComposite;
	private final List<SqlSelection> sqlSelections;
	private final CompositeType compositeType;

	public ReturnAssemblerComposite(
			ReturnCompositeImpl returnComposite,
			List<SqlSelection> sqlSelections,
			CompositeType compositeType) {

		this.returnComposite = returnComposite;
		this.sqlSelections = sqlSelections;
		this.compositeType = compositeType;
	}

	@Override
	public Class getReturnedJavaType() {
		return returnComposite.getReturnedJavaType();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		// for now has to be a very basic CompositeType (aka one-level && attributes of BasicType)
		final Object[] values = new Object[ sqlSelections.size() ];
		for ( int i = 0; i < sqlSelections.size(); i++ ) {
			values[i] = rowProcessingState.getJdbcValues()[ sqlSelections.get( i ).getValuesArrayPosition() ];
		}

		try {
			final Object result = compositeType.getReturnedClass().newInstance();
			compositeType.setPropertyValues( result, values, EntityMode.POJO );
			return result;
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to instantiate composite : " +  compositeType.getReturnedClass().getName(), e );
		}
	}
}
