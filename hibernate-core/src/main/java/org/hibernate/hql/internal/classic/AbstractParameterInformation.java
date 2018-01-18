/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.hql.spi.ParameterInformation;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.param.ParameterBinder;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractParameterInformation implements ParameterInformation, ParameterBinder {
	private List<Integer> sqlPositions = new ArrayList<>();

	@Override
	public int[] getSourceLocations() {
		return ArrayHelper.toIntArray( sqlPositions );
	}

	public void addSourceLocation(int position) {
		sqlPositions.add( position );
	}

	@Override
	public Type getExpectedType() {
		// the classic translator does not know this information
		return null;
	}

	@Override
	public void setExpectedType(Type expectedType) {
		// nothing to do - classic translator does not know this information
	}
}
