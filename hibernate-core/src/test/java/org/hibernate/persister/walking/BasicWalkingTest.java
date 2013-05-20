/*
 * jDocBook, processing of DocBook sources
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.persister.walking;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationVisitationStrategy;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.CompositionElementDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.MetadataDrivenModelGraphVisitor;

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
		MetadataDrivenModelGraphVisitor.visitEntity(
				new AssociationVisitationStrategy() {
					private int depth = 0;

					@Override
					public void start() {
						System.out.println( ">> Start" );
					}

					@Override
					public void finish() {
						System.out.println( "<< Finish" );
					}

					@Override
					public void startingEntity(EntityDefinition entityDefinition) {
						System.out.println(
								String.format(
										"%s Starting entity (%s)",
										StringHelper.repeat( ">>", ++depth ),
										entityDefinition.toString()
								)
						);
					}

					@Override
					public void finishingEntity(EntityDefinition entityDefinition) {
						System.out.println(
								String.format(
										"%s Finishing entity (%s)",
										StringHelper.repeat( "<<", depth-- ),
										entityDefinition.toString()
								)
						);
					}

					@Override
					public void startingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
						//To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void finishingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
						//To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void startingCollection(CollectionDefinition collectionDefinition) {
						System.out.println(
								String.format(
										"%s Starting collection (%s)",
										StringHelper.repeat( ">>", ++depth ),
										collectionDefinition.toString()
								)
						);
					}

					@Override
					public void finishingCollection(CollectionDefinition collectionDefinition) {
						System.out.println(
								String.format(
										"%s Finishing collection (%s)",
										StringHelper.repeat( ">>", depth-- ),
										collectionDefinition.toString()
								)
						);
					}

					@Override
					public void startingCollectionIndex(CollectionIndexDefinition collectionIndexDefinition) {
						//To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void finishingCollectionIndex(CollectionIndexDefinition collectionIndexDefinition) {
						//To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void startingCollectionElements(CollectionElementDefinition elementDefinition) {
						//To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void finishingCollectionElements(CollectionElementDefinition elementDefinition) {
						//To change body of implemented methods use File | Settings | File Templates.
					}

					@Override
					public void startingComposite(CompositionDefinition compositionDefinition) {
						System.out.println(
								String.format(
										"%s Starting composite (%s)",
										StringHelper.repeat( ">>", ++depth ),
										compositionDefinition.toString()
								)
						);
					}

					@Override
					public void finishingComposite(CompositionDefinition compositionDefinition) {
						System.out.println(
								String.format(
										"%s Finishing composite (%s)",
										StringHelper.repeat( ">>", depth-- ),
										compositionDefinition.toString()
								)
						);
					}

					@Override
					public void startingCompositeElement(CompositionElementDefinition compositionElementDefinition) {
						System.out.println(
								String.format(
										"%s Starting composite (%s)",
										StringHelper.repeat( ">>", ++depth ),
										compositionElementDefinition.toString()
								)
						);
					}

					@Override
					public void finishingCompositeElement(CompositionElementDefinition compositionElementDefinition) {
						System.out.println(
								String.format(
										"%s Finishing composite (%s)",
										StringHelper.repeat( ">>", depth-- ),
										compositionElementDefinition.toString()
								)
						);
					}

					@Override
					public boolean startingAttribute(AttributeDefinition attributeDefinition) {
						System.out.println(
								String.format(
										"%s Handling attribute (%s)",
										StringHelper.repeat( ">>", depth + 1 ),
										attributeDefinition.toString()
								)
						);
						return true;
					}

					@Override
					public void finishingAttribute(AttributeDefinition attributeDefinition) {
						// nothing to do
					}
				},
				ep
		);
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
