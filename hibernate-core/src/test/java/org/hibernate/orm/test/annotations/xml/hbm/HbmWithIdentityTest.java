/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.xml.hbm;


import org.hibernate.dialect.HANADialect;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(
		annotatedClasses = { Sky.class, ZImpl.class },
		xmlMappings = {
				"org/hibernate/orm/test/annotations/xml/hbm/A.hbm.xml",
				"org/hibernate/orm/test/annotations/xml/hbm/B.hbm.xml",
				"org/hibernate/orm/test/annotations/xml/hbm/CloudType.hbm.xml"
		}
)
@SessionFactory
public class HbmWithIdentityTest {
	@Test
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testManyToOneAndInterface(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					B b = new BImpl();
					b.setBId( 1 );
					s.persist( b );
					Z z = new ZImpl();
					z.setB( b );
					s.persist( z );
					s.flush();
				}
		);
	}
}
