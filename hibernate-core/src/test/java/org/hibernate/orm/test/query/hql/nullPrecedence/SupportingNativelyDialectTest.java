/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql.nullPrecedence;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * @author Nathan Xu
 */
@DomainModel( annotatedClasses = ExampleEntity.class )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportNullPrecedence.class )
public class SupportingNativelyDialectTest extends AbstractNullPrecedenceTest {
}
