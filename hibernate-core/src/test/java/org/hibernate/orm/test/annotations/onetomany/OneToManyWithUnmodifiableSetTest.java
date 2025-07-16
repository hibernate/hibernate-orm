/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import org.assertj.core.api.InstanceOfAssertFactories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Allows testing a collection accessed via a getter that returns a {@link Collections#unmodifiableSet(Set)}
 *
 * @author Vincent Bouthinon
 */
@DomainModel(
		annotatedClasses = {
				OneToManyWithUnmodifiableSetTest.Client.class,
				OneToManyWithUnmodifiableSetTest.Command.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-19589")
public class OneToManyWithUnmodifiableSetTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Client client = new Client();
			Command command = new Command();
			client.addToCommands( command );
			session.persist( client );
			session.persist( command );
			session.flush();
			session.clear();
			Client clientFind = session.find( Client.class, client.id );
			assertThat( clientFind )
					.isNotNull()
					.extracting( Client::getCommands )
					.asInstanceOf( InstanceOfAssertFactories.COLLECTION )
					.isNotEmpty();
		} );
	}


	@Entity(name = "Client")
	public static class Client {
		@Transient
		protected Set<Command> commands = new HashSet<>();
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany
		@Access(AccessType.PROPERTY)
		public Set<Command> getCommands() {
			return Collections.unmodifiableSet( commands );
		}

		public void setCommands(Set<Command> commands) {
			this.commands = commands;
		}

		public void addToCommands(Command command) {
			this.commands.add( command );
		}

		/**
		 * It triggers the {@link org.hibernate.event.internal.DefaultFlushEntityEventListener#copyState} method, because otherwise, if there’s no callbackPreUpdate,
		 * the system assumes the entity isn’t dirty and doesn’t perform copyState to check for dirtiness.
		 */
		@PreUpdate
		public void preUpdate() {
		}
	}

	@Entity(name = "Command")
	public static class Command {

		@Id
		@GeneratedValue
		private Long id;
	}
}
