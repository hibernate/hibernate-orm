/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.hql.spi.NamedParameterInformation;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class NamedParameterInformationImpl implements NamedParameterInformation {
	private final String name;

	private final List<Integer> sqlPositions = new ArrayList<>();

	private Type expectedType;

	NamedParameterInformationImpl(String name, Type initialType) {
		this.name = name;
		this.expectedType = initialType;
	}

	@Override
	public String getSourceName() {
		return name;
	}

	@Override
	public int[] getSourceLocations() {
		return ArrayHelper.toIntArray( sqlPositions );
	}

	@Override
	public Type getExpectedType() {
		return expectedType;
	}

	public void addSourceLocation(int position) {
		sqlPositions.add( position );
	}

	public void setExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}
}
