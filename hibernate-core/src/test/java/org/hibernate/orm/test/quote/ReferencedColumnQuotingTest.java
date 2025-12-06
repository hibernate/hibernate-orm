/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				ReferencedColumnQuotingTest.User.class,
				ReferencedColumnQuotingTest.Position2.class,
				ReferencedColumnQuotingTest.JobPosition.class,
				ReferencedColumnQuotingTest.Job2Position.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
)
@Jira("https://hibernate.atlassian.net/browse/HHH-9035")
public class ReferencedColumnQuotingTest {

	@Test
	public void testExplicitQuoting(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					User user = new User();
					user.id = 1L;
					user.name = "User 1";
					session.persist(user);
					Position2 position = new Position2();
					position.id = 1L;
					position.name = "Position 1";
					position.user = user;
					session.persist(position);
				}
		);
	}

	@Entity
	@IdClass(Job2PositionId.class)
	public static class Job2Position {
		Long id;
		JobPosition pos;
		@Id
		public Long getId() {
			return this.id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		@Id
		@ManyToOne
		@JoinColumns({
				@JoinColumn(name = "`User+id`", referencedColumnName = "`User+id`", insertable = false, updatable = false),
				@JoinColumn(name = "`Position+id`", referencedColumnName = "`Position+id`", insertable = false, updatable = false),
				@JoinColumn(name = "`JobPosition+id`", referencedColumnName = "`JobPosition+id`", insertable = false, updatable = false),
		})
		public JobPosition getPos() {
			return this.pos;
		}
		public void setPos(JobPosition pos) {
			this.pos = pos;
		}
	}

	public static class Job2PositionId {
		Long id;
		JobPositionId pos;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public JobPositionId getPos() {
			return pos;
		}

		public void setPos(JobPositionId pos) {
			this.pos = pos;
		}
	}

	@Entity
	@Table(name = "JOB+Position")
	@IdClass(JobPositionId.class)
	public static class JobPosition {
		@Id
		@Column(name = "`JobPosition+id`")
		Long id;
		@ManyToOne
		@JoinColumn(name = "`User+id`", referencedColumnName = "`User+id`")
		@Id
		User user;
		@ManyToOne
		@JoinColumns({
				@JoinColumn(name = "`User+id`", referencedColumnName = "`User+id`", insertable = false, updatable = false),
				@JoinColumn(name = "`Position+id`", referencedColumnName = "`Position+id`", insertable = false, updatable = false),
		})
		Position2 position;
		@Id
		@Column(name = "`Position+id`")
		Long positionId;
	}

	public static class JobPositionId {
		Long id;
		Long user;
		Long positionId;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getUser() {
			return user;
		}

		public void setUser(Long user) {
			this.user = user;
		}

		public Long getPositionId() {
			return positionId;
		}

		public void setPositionId(Long positionId) {
			this.positionId = positionId;
		}
	}

	@Entity
	@Table(name = "Position")
	@IdClass(PositionId.class)
	public static class Position2 {
		@Id
		@Column(name = "`Position+id`")
		Long id;
		@Column(name = "position++name")
		String name;
		@ManyToOne
		@JoinColumn(name = "`User+id`", referencedColumnName = "`User+id`")
		@Id
		User user;
	}

	public static class PositionId {
		Long id;
		Long user;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getUser() {
			return user;
		}

		public void setUser(Long user) {
			this.user = user;
		}
	}

	@Entity
	@Table(name = "User")
	public static class User {
		@Id
		@Column(name = "User+id")
		Long id;
		@Column(name = "Name")
		String name;
	}

}
