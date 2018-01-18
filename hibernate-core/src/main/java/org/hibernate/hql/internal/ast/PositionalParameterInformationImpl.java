/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.hql.spi.PositionalParameterInformation;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class PositionalParameterInformationImpl implements PositionalParameterInformation {
	private final int label;

	private final  List<Integer> sourceLocations = new ArrayList<>();

	private Type expectedType;

	public PositionalParameterInformationImpl(int label, Type initialType) {
		this.label = label;
		this.expectedType = initialType;
	}

	@Override
	public int getLabel() {
		return label;
	}

	@Override
	public int[] getSourceLocations() {
		return ArrayHelper.toIntArray( sourceLocations );
	}

	@Override
	public Type getExpectedType() {
		return expectedType;
	}

	@Override
	public void setExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public void addSourceLocation(int location) {
		sourceLocations.add( location );
	}
}
