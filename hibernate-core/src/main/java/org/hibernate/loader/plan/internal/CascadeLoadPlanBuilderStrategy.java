/*
 * jDocBook, processing of DocBook sources
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
package org.hibernate.loader.plan.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;

/**
 * A LoadPlan building strategy for cascade processing; meaning, it builds the LoadPlan for loading related to
 * cascading a particular action across associations
 *
 * @author Steve Ebersole
 */
public class CascadeLoadPlanBuilderStrategy extends SingleRootReturnLoadPlanBuilderStrategy {
	private static final FetchStrategy EAGER = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );
	private static final FetchStrategy DELAYED = new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT );

	private final CascadingAction cascadeActionToMatch;

	public CascadeLoadPlanBuilderStrategy(
			CascadingAction cascadeActionToMatch,
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( sessionFactory, loadQueryInfluencers );
		this.cascadeActionToMatch = cascadeActionToMatch;
	}

	@Override
	protected FetchStrategy determineFetchPlan(AssociationAttributeDefinition attributeDefinition) {
		return attributeDefinition.determineCascadeStyle().doCascade( cascadeActionToMatch ) ? EAGER : DELAYED;
	}
}
