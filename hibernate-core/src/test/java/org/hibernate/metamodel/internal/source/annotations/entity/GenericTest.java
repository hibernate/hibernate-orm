/*
 * Hibernate, Relational Persistence for Idiomatic Java
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class GenericTest extends BaseAnnotationBindingTestCase {
	@Entity
	@Inheritance
	public abstract class Event {
		private long id;

		@Id
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	public class LoginEvent extends Event {

	}

	@Entity
	@Inheritance
	public abstract class Notification<X extends Event> {
		private long id;
		private X event;

		@ManyToOne(optional = false, targetEntity = Event.class)
		@JoinColumn
		public X getEvent() {
			return event;
		}

		public void setEvent(X event) {
			this.event = event;
		}

		@Id
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	public class LoginNotification extends Notification<LoginEvent> {

	}

	@Test
	@Ignore("not finished yet")
	@Resources(annotatedClasses = { LoginNotification.class, Notification.class, LoginEvent.class, Event.class })
	public void testGenericProperty() {
		EntityBinding entityBinding = getEntityBinding( LoginNotification.class );
		AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( "event" );
//		Assert.assertNotNull( attributeBinding );

	}
}
