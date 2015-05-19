/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
