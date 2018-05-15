/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.walking;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.MetamodelGraphWalker;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class BasicWalkingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Message.class, Poster.class };
	}

	@Test
	public void testIt() {
		EntityPersister ep = (EntityPersister) sessionFactory().getClassMetadata(Message.class);
		MetamodelGraphWalker.visitEntity( new LoggingAssociationVisitationStrategy(), ep );
	}

	@Entity( name = "Message" )
	public static class Message {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		@JoinColumn
		private Poster poster;
	}

	@Entity( name = "Poster" )
	public static class Poster {
		@Id
		private Integer id;
		private String name;
		@OneToMany(mappedBy = "poster")
		private List<Message> messages;
	}
}
