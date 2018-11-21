/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows a test to indicate a {@link DomainModel} it uses via {@link AvailableDomainModel}.
 *
 * These models are then automatically applied on behalf of the test by our JUnit
 * extensions.  The ultimate goal is to allow the tests being executed to share
 * the Connection pool and exported schema(s) for the overall suite execution.  Both of
 * these operations are generally very heavy operations and are a major contributor to the
 * excessive amount of time to run the Hibernate test suite in its current set up (each
 * test creating the pools and exporting the schema.
 *
 * Note that this would allow tests not sharing a schema can be executed in parallel since there
 * is no danger of "data collision" - meaning there is no danger that multiple tests will be reading
 * or writing the same table(s) simultaneously leading to incorrect results (skewed counts, etc).
 *
 * Would work best with the ability to either drop a namespace or truncate (or delete) tables.  Deleting
 * tables is also an option in lieu of truncating for databases that do not support truncation.
 *
 * NOTE : namespace = catalog/schema
 *
 * @see AvailableDomainModel
 * @see DomainModel
 * @see TestDomainGroup
 *
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable( TestDomainGroup.class  )
public @interface TestDomain {
	/**
	 * The AvailableDomainModel to apply
	 */
	AvailableDomainModel value();
}
