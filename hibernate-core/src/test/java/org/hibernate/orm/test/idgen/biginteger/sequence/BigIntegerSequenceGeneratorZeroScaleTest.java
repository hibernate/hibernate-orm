/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.biginteger.sequence;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * This test explicitly maps scale="0" for the sequence column. It works around the arithmetic
 * overflow that happens because the generated column cannot accommodate the SQL Server
 * sequence that starts, by default, with the value, -9,223,372,036,854,775,808.
 *
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/idgen/biginteger/sequence/ZeroScaleMapping.hbm.xml")
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSequences.class )
public class BigIntegerSequenceGeneratorZeroScaleTest extends BigIntegerSequenceGeneratorTest {
	@Test
	@JiraKey( value = "HHH-9250")
	public void testBasics(SessionFactoryScope scope) {
		super.testBasics( scope );
	}
}
