/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import java.util.Date;
import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@JiraKey(value = "HHH-14529")
@DomainModel(
		annotatedClasses = { CarModel.class, Manufacturer.class, Model.class, Light.class },
		xmlMappings = {
				"org/hibernate/orm/test/annotations/xml/ejb3/orm.xml",
				"org/hibernate/orm/test/annotations/xml/ejb3/orm2.xml",
				"org/hibernate/orm/test/annotations/xml/ejb3/orm3.xml",
				"org/hibernate/orm/test/annotations/xml/ejb3/orm4.xml"
		}
)
@SessionFactory
public class Ejb3XmlTest {

	@Test
	@SkipForDialect( dialectClass = PostgreSQLDialect.class,
			reason = "driver does not implement the setQueryTimeout method" )
	@SkipForDialect( dialectClass = CockroachDialect.class,
			reason = "driver does not implement the setQueryTimeout method" )
	public void testEjb3Xml(SessionFactoryScope scope) {
		scope.inSession( (s) -> {
			s.getTransaction().begin();

			CarModel model = new CarModel();
			model.setYear( new Date() );
			Manufacturer manufacturer = new Manufacturer();
			//s.persist( manufacturer );
			model.setManufacturer( manufacturer );
			manufacturer.getModels().add( model );
			s.persist( model );
			s.flush();
			s.clear();

			model.setYear( new Date() );
			manufacturer = s.get( Manufacturer.class, manufacturer.getId() );
			@SuppressWarnings("unchecked")
			List<Model> cars = s.getNamedQuery( "allModelsPerManufacturer" )
					.setParameter( "manufacturer", manufacturer )
					.list();
			assertEquals( 1, cars.size() );
			for ( Model car : cars ) {
				Assertions.assertNotNull( car.getManufacturer() );
				s.remove( manufacturer );
				s.remove( car );
			}

			s.getTransaction().rollback();
		} );
	}

	@Test
	public void testXMLEntityHandled(SessionFactoryScope scope) {
		scope.inSession( (s) -> {
			s.getTransaction().begin();
			Lighter l = new Lighter();
			l.name = "Blue";
			l.power = "400F";
			s.persist( l );
			s.flush();
			s.getTransaction().rollback();
		} );
	}

	@Test
	public void testXmlDefaultOverriding(SessionFactoryScope scope) {
		scope.inSession( (s) -> {
			Transaction tx = s.beginTransaction();
			Manufacturer manufacturer = new Manufacturer();
			s.persist( manufacturer );
			s.flush();
			s.clear();

			assertEquals( 1, s.getNamedQuery( "manufacturer.findAll" ).list().size() );

			tx.rollback();
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapXMLSupport(SessionFactoryScope scope) {
		scope.inSession( (s) -> {
			SessionFactoryImplementor sf = s.getSessionFactory();
			Transaction tx = s.beginTransaction();

			// Verify that we can persist an object with a couple Map mappings
			VicePresident vpSales = new VicePresident();
			vpSales.name = "Dwight";
			Company company = new Company();
			company.conferenceRoomExtensions.put( "8932", "x1234" );
			company.organization.put( "sales", vpSales );
			s.persist( company );
			s.flush();
			s.clear();

			// For the element-collection, check that the orm.xml entries are honored.
			// This includes: map-key-column/column/collection-table/join-column
			BasicCollectionPersister confRoomMeta = (BasicCollectionPersister) sf
					.unwrap( SessionFactoryImplementor.class )
					.getMappingMetamodel()
					.getCollectionDescriptor( Company.class.getName() + ".conferenceRoomExtensions" );
			assertEquals( "company_id", confRoomMeta.getKeyColumnNames()[0] );
			assertEquals( "phone_extension", confRoomMeta.getElementColumnNames()[0] );
			assertEquals( "room_number", confRoomMeta.getIndexColumnNames()[0] );
			assertEquals( "phone_extension_lookup", confRoomMeta.getTableName() );
			tx.rollback();
		} );
	}
}
