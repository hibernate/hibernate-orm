/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.source.hbm;

import org.hibernate.internal.jaxb.mapping.hbm.JaxbFilterElement;
import org.hibernate.metamodel.spi.source.FilterSource;

/**
 * @author Steve Ebersole
 */
public class FilterSourceImpl implements FilterSource {
	private final String name;
	private final String condition;

	public FilterSourceImpl(JaxbFilterElement filterElement) {
		this.name = filterElement.getName();

		String conditionAttribute = filterElement.getCondition();
		String conditionContent = filterElement.getValue();
		this.condition = Helper.coalesce( conditionContent, conditionAttribute );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getCondition() {
		return condition;
	}
}
