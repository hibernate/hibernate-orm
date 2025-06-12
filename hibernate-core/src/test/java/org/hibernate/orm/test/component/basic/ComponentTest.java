/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.basic;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModelExtension;
import org.hibernate.testing.orm.junit.DomainModelProducer;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@ExtendWith( DomainModelExtension.class )
@SessionFactory(generateStatistics = true)
public class ComponentTest implements DomainModelProducer {

	@Override
	public MetadataImplementor produceModel(StandardServiceRegistry serviceRegistry) {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addResource( "org/hibernate/orm/test/component/basic/User.xml" )
				.buildMetadata();
		adjustYob(
				metadata.getEntityBinding( User.class.getName() ),
				metadata.getDatabase().getJdbcEnvironment().getDialect()
		);
		adjustYob(
				metadata.getEntityBinding( Employee.class.getName() ),
				metadata.getDatabase().getJdbcEnvironment().getDialect()
		);

		return metadata;
	}

	public static void adjustYob(PersistentClass entityBinding, Dialect dialect) {
		// Oracle and Postgres do not have year() functions, so we need to
		// redefine the 'Person.yob' formula
		//
		// consider temporary until we add the capability to define
		// mapping formulas which can use dialect-registered functions...

		Property personProperty = entityBinding.getProperty( "person" );
		Component component = ( Component ) personProperty.getValue();
		Formula f = ( Formula ) component.getProperty( "yob" ).getValue().getSelectables().get( 0 );

		String pattern = dialect.extractPattern( TemporalUnit.YEAR );
		String formula = pattern.replace( "?1", "YEAR" ).replace( "?2", "dob" );
		f.setFormula( formula );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testUpdateFalse(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		sessionFactory.getStatistics().clear();

		factoryScope.inTransaction(
				s -> {
					User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
					s.persist(u);
					s.flush();
					u.getPerson().setName("XXXXYYYYY");
				}
		);

		assertEquals( 1, sessionFactory.getStatistics().getEntityInsertCount() );
		assertEquals( 0, sessionFactory.getStatistics().getEntityUpdateCount() );

		factoryScope.inTransaction(
				s -> {
					User u = s.find( User.class, "gavin" );
					assertEquals( "Gavin King", u.getPerson().getName() );
				}
		);
	}


	@Test
	public void testComponent(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(
				s -> {
					User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
					s.persist(u);
					s.flush();
					u.getPerson().changeAddress("Phipps Place");
				}
		);

		factoryScope.inTransaction(
				s -> {
					User u = s.find( User.class, "gavin" );
					assertEquals( u.getPerson().getAddress(), "Phipps Place" );
					assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
					assertEquals( u.getPerson().getYob(), u.getPerson().getDob().getYear()+1900 );
					u.setPassword("$ecret");
				}
		);

		factoryScope.inTransaction(
				s -> {
					User u = s.find( User.class, "gavin" );
					assertEquals( u.getPerson().getAddress(), "Phipps Place" );
					assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
					assertEquals( u.getPassword(), "$ecret" );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-2366" )
	public void testComponentStateChangeAndDirtiness(SessionFactoryScope factoryScope) {
		final StatisticsImplementor statistics = factoryScope.getSessionFactory().getStatistics();
		statistics.clear();

		factoryScope.inTransaction(
				s -> {
					User u = new User( "steve", "hibernater", new Person( "Steve Ebersole", new Date(), "Main St") );
					s.persist( u );
					s.flush();
					long intialUpdateCount = statistics.getEntityUpdateCount();
					u.getPerson().setAddress( "Austin" );
					s.flush();
					assertEquals( intialUpdateCount + 1, statistics.getEntityUpdateCount() );
					intialUpdateCount = statistics.getEntityUpdateCount();
					u.getPerson().setAddress( "Cedar Park" );
					s.flush();
					assertEquals( intialUpdateCount + 1, statistics.getEntityUpdateCount() );
				}
		);
	}

	@Test
	public void testCustomColumnReadAndWrite(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(
				s -> {
					User u = new User( "steve", "hibernater", new Person( "Steve Ebersole", new Date(), "Main St") );
					final double HEIGHT_INCHES = 73;
					final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;
					u.getPerson().setHeightInches(HEIGHT_INCHES);
					s.persist( u );
					s.flush();

					// Test value conversion during insert
					// Value returned by Oracle native query is a Types.NUMERIC, which is mapped to a BigDecimalType;
					// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
					Double heightViaSql =
							( (Number)s.createNativeQuery("select height_centimeters from t_users where t_users.userName='steve'").uniqueResult())
									.doubleValue();
					assertEquals(HEIGHT_CENTIMETERS, heightViaSql, 0.01d);

					// Test projection
					Double heightViaHql = (Double)s.createQuery("select u.person.heightInches from User u where u.id = 'steve'").uniqueResult();
					assertEquals(HEIGHT_INCHES, heightViaHql, 0.01d);

					// Test restriction and entity load via criteria
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
					Root<User> root = criteria.from( User.class );
					Join<Object, Object> person = root.join( "person", JoinType.INNER );
					criteria.where( criteriaBuilder.between( person.get( "heightInches" ), HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d) );
					u = s.createQuery( criteria ).uniqueResult();
//		u = (User)s.createCriteria(User.class)
//			.add(Restrictions.between("person.heightInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d))
//			.uniqueResult();
					assertEquals(HEIGHT_INCHES, u.getPerson().getHeightInches(), 0.01d);

					// Test predicate and entity load via HQL
					u = (User)s.createQuery("from User u where u.person.heightInches between ?1 and ?2")
							.setParameter(1, HEIGHT_INCHES - 0.01d)
							.setParameter(2, HEIGHT_INCHES + 0.01d)
							.uniqueResult();
					assertEquals(HEIGHT_INCHES, u.getPerson().getHeightInches(), 0.01d);

					// Test update
					u.getPerson().setHeightInches(1);
					s.flush();
					heightViaSql =
							( (Number)s.createNativeQuery("select height_centimeters from t_users where t_users.userName='steve'").uniqueResult() )
									.doubleValue();
					assertEquals(2.54d, heightViaSql, 0.01d);
					s.remove(u);
				}
		);
	}

	@Test
	public void testNamedQuery(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(
				s -> s.getNamedQuery( "userNameIn")
						.setParameterList( "nameList", new Object[] {"1ovthafew", "turin", "xam"} )
						.list()
		);
	}
}
