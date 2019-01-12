/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm;

import java.util.Set;
import javax.money.MonetaryAmount;

import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.SelectStatement;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.helpdesk.HelpDeskDomainModel;
import org.hibernate.testing.orm.domain.helpdesk.Status;
import org.hibernate.testing.orm.domain.retail.RetailDomainModel;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for "type inference specifically limited to the cases JPA
 * says should be supported.
 *
 * @see ExtensionSqmInferenceTests
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class JpaStandardSqmInferenceTests extends SessionFactoryBasedFunctionalTest {
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		HelpDeskDomainModel.INSTANCE.applyDomainModel( metadataSources );
		RetailDomainModel.INSTANCE.applyDomainModel( metadataSources );
	}

	@Test
	public void testEnumInference() {
		checkParameters(
				"from Account a where a.loginStatus = :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus <> :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus != :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus > :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus >= :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus < :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus <= :status",
				Status.class
		);
	}

	@Test
	public void testConvertedInference() {
		checkParameters(
				"select l from LineItem l where l.subTotal = :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal <> :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal != :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal > :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal >= :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal < :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal <= :limit",
				MonetaryAmount.class
		);
	}

	private void checkParameters(String query, Class<?>... expectedTypes) {
		final SqmSelectStatement sqmStatement = (SqmSelectStatement) sessionFactory().getQueryEngine().getSemanticQueryProducer().interpret( query );
		final SqmSelectToSqlAstConverter converter = new SqmSelectToSqlAstConverter(
				QueryOptions.NONE,
				new SqlAstProducerContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return sessionFactory();
					}

					@Override
					public LoadQueryInfluencers getLoadQueryInfluencers() {
						return LoadQueryInfluencers.NONE;
					}

					@Override
					public Callback getCallback() {
						return afterLoadAction -> {};
					}
				}
		);

		converter.visitSelectStatement( sqmStatement );

		checkParameterTypes( sqmStatement, converter, expectedTypes );
	}

	private void checkParameterTypes(
			SqmSelectStatement sqmStatement,
			SqmSelectToSqlAstConverter converter,
			Class<?>[] expectedTypes) {
		final Set<SqmParameter> sqmParameters = sqmStatement.getQueryParameters();
		assertEquals( expectedTypes.length, sqmParameters.size() );

		// to be completely correct, the `getSqmParamResolvedTypeMap` would need to be sorted
		int count = 0;
		for ( SqmParameter sqmParameter : sqmParameters ) {
			final AllowableParameterType<?> resolvedType = converter.getSqmParamResolvedTypeMap().get( sqmParameter );

			assertEquals(
					"Query parameter type : " + sqmParameter,
					expectedTypes[count++],
					resolvedType.getJavaType()
			);
//			assertThat(
//					queryParameter.getAnticipatedType().getJavaType(),
//					AssignableMatcher.assignableTo( expectedParameterTypes[count++] )
//			);
		}
	}
}
