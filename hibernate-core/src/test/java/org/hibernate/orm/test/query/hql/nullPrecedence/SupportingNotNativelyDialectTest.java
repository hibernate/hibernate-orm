/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportNullPrecedence.class, reverse = true )
public class SupportingNotNativelyDialectTest extends AbstractNullPrecedenceTest {
}
