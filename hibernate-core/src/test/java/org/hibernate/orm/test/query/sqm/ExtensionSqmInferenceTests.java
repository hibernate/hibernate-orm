/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.boot.MetadataSources;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.helpdesk.HelpDeskDomainModel;
import org.hibernate.testing.orm.domain.retail.RetailDomainModel;

/**
 * Tests for type inference outside of what JPA says should be supported.
 *
 * NOTE : Distinguishing this from {@link JpaStandardSqmInferenceTests} allows
 * applying {@link JpaCompliance#isJpaQueryComplianceEnabled()} testing for just
 * these extensions
 *
 * @see JpaStandardSqmInferenceTests
 *
 * @author Steve Ebersole
 */
public class ExtensionSqmInferenceTests extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		HelpDeskDomainModel.INSTANCE.applyDomainModel( metadataSources );
		RetailDomainModel.INSTANCE.applyDomainModel( metadataSources );
	}

	// todo (6.0) : add the checks ;)
}
