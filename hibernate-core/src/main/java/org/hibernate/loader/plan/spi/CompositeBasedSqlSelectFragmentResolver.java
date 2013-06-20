/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

import java.util.Arrays;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class CompositeBasedSqlSelectFragmentResolver implements SqlSelectFragmentResolver {
	protected static interface BaseSqlSelectFragmentResolver {
		public String[] toSqlSelectFragments(String alias);
	}

	public CompositeBasedSqlSelectFragmentResolver(
			SessionFactoryImplementor sessionFactory, CompositeType compositeType,
			BaseSqlSelectFragmentResolver baseResolver) {
		this.sessionFactory = sessionFactory;
		this.compositeType = compositeType;
		this.baseResolver = baseResolver;
	}

	private final SessionFactoryImplementor sessionFactory;
	private final CompositeType compositeType;
	private final BaseSqlSelectFragmentResolver baseResolver;

	@Override
	public String[] toSqlSelectFragments(String alias, AttributeDefinition attributeDefinition) {
		int subIndex = -1;
		int selectFragmentRangeStart = 0;
		int selectFragmentRangeEnd = -1;

		for ( int i = 0; i < compositeType.getPropertyNames().length; i++ ) {
			final Type type = compositeType.getSubtypes()[i];
			final int typeColSpan = type.getColumnSpan( sessionFactory );
			if ( compositeType.getPropertyNames()[ i ].equals( attributeDefinition.getName() ) ) {
				// fount it!
				subIndex = i;
				selectFragmentRangeEnd = selectFragmentRangeStart + typeColSpan;
				break;
			}
			selectFragmentRangeStart += typeColSpan;
		}

		if ( subIndex < 0 ) {
			throw new WalkingException(
					String.format(
							"Owner property [%s] not found in composite properties [%s]",
							attributeDefinition.getName(),
							Arrays.asList( compositeType.getPropertyNames() )
					)
			);
		}

		return Arrays.copyOfRange(
				baseResolver.toSqlSelectFragments( alias ),
				selectFragmentRangeStart,
				selectFragmentRangeEnd
		);
	}
}
