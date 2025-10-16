/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.entityNames.auditedEntity;

import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Hern&aacute;n Chanfreau
 */
@RequiresDialect(H2Dialect.class)
@DomainModel(xmlMappings = "mappings/entityNames/auditedEntity/mappings.hbm.xml")
@SessionFactory
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReadEntityWhitEntityNameTest {

	private long id_pers1;
	private long id_pers2;
	private long id_pers3;

	private Person person1_1;
	private Person person1_2;
	private Person person1_3;

	private Person currentPers1;

	@Test
	@Order(1)
	public void testObtainEntityNameAuditedEntityWithEntityName(SessionFactoryScope scope) {
		scope.inSession( session -> {
			Person pers1 = new Person( "Hernan", 28 );
			Person pers2 = new Person( "Leandro", 29 );
			Person pers3 = new Person( "Barba", 30 );

			//REV 1
			session.getTransaction().begin();
			session.persist( "Personaje", pers1 );
			id_pers1 = pers1.getId();
			session.getTransaction().commit();

			//REV 2
			session.getTransaction().begin();
			pers1 = (Person) session.get( "Personaje", id_pers1 );
			pers1.setAge( 29 );
			session.persist( "Personaje", pers1 );
			session.persist( "Personaje", pers2 );
			id_pers2 = pers2.getId();
			session.getTransaction().commit();

			//REV
			session.getTransaction().begin();
			pers1 = (Person) session.get( "Personaje", id_pers1 );
			pers1.setName( "Hernan David" );
			pers2 = (Person) session.get( "Personaje", id_pers2 );
			pers2.setAge( 30 );
			session.persist( "Personaje", pers1 );
			session.persist( "Personaje", pers2 );
			session.persist( "Personaje", pers3 );
			id_pers3 = pers3.getId();
			session.getTransaction().commit();

			session.getTransaction().begin();
			currentPers1 = (Person) session.get( "Personaje", id_pers1 );
			session.getTransaction().commit();

			final var auditReader = AuditReaderFactory.get( session );
			testRetrieveRevisionsWithEntityName( auditReader );
			testRetrieveAuditedEntityWithEntityName( auditReader );
			checkEntityNames( session, auditReader );
		} );
	}

	public void testRetrieveRevisionsWithEntityName(AuditReader auditReader) {
		List<Number> pers1Revs = auditReader.getRevisions( Person.class, "Personaje", id_pers1 );
		List<Number> pers2Revs = auditReader.getRevisions( Person.class, "Personaje", id_pers2 );
		List<Number> pers3Revs = auditReader.getRevisions( Person.class, "Personaje", id_pers3 );

		assertThat( pers1Revs ).hasSize( 3 );
		assertThat( pers2Revs ).hasSize( 2 );
		assertThat( pers3Revs ).hasSize( 1 );
	}

	public void testRetrieveAuditedEntityWithEntityName(AuditReader auditReader) {
		person1_1 = auditReader.find( Person.class, "Personaje", id_pers1, 1 );
		person1_2 = auditReader.find( Person.class, "Personaje", id_pers1, 2 );
		person1_3 = auditReader.find( Person.class, "Personaje", id_pers1, 3 );

		assertThat( person1_1 ).isNotNull();
		assertThat( person1_2 ).isNotNull();
		assertThat( person1_3 ).isNotNull();
	}

	private void checkEntityNames(SessionImplementor session, AuditReader auditReader) {
		String currentPers1EN = session.getEntityName( currentPers1 );

		String person1EN = auditReader.getEntityName( person1_1.getId(), 1, person1_1 );
		assertThat( person1EN ).isEqualTo( currentPers1EN );

		String person2EN = auditReader.getEntityName( person1_2.getId(), 2, person1_2 );
		assertThat( person2EN ).isEqualTo( currentPers1EN );

		String person3EN = auditReader.getEntityName( person1_3.getId(), 3, person1_3 );
		assertThat( person3EN ).isEqualTo( currentPers1EN );
	}

	@Test
	public void testRetrieveAuditedEntityWithEntityNameWithNewSession(SessionFactoryScope scope) {
		scope.inSession(  session -> {
			// force a new session and AR
			final var auditReader = AuditReaderFactory.get( session );
			testRetrieveRevisionsWithEntityName( auditReader );
			testRetrieveAuditedEntityWithEntityName( auditReader );
		} );
	}
}
