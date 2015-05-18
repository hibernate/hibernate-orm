/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

/**
 * TypeContributor for adding Java8 Date/Time specific Type implementations
 *
 * @author Steve Ebersole
 */
public class Java8DateTimeTypeContributor implements TypeContributor {
	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeType( LocalDateTimeType.INSTANCE );
		typeContributions.contributeType( LocalDateType.INSTANCE );
		typeContributions.contributeType( LocalTimeType.INSTANCE );

		typeContributions.contributeType( InstantType.INSTANCE );

		typeContributions.contributeType( ZonedDateTimeType.INSTANCE );
		typeContributions.contributeType( OffsetDateTimeType.INSTANCE );
		typeContributions.contributeType( OffsetTimeType.INSTANCE );

		typeContributions.contributeType( DurationType.INSTANCE );
	}
}
