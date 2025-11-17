/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.warning;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.LoggingInspections;
import org.hibernate.testing.orm.junit.LoggingInspectionsScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.LockModeType.NONE;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10513")
@LoggingInspections(messages = {
		@LoggingInspections.Message(
				messageKey = "HHH000444",
				loggers = @Logger(loggerName = CoreMessageLogger.NAME)
		),
		@LoggingInspections.Message(
				messageKey = "HHH000445",
				loggers = @Logger(loggerName = CoreMessageLogger.NAME)
		)
})
@DomainModel(annotatedClasses = {LockNoneWarningTest.Item.class, LockNoneWarningTest.Bid.class})
@SessionFactory
public class LockNoneWarningTest {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			Item item = new Item();
			item.name = "ZZZZ";
			session.persist( item );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	/// Test that no warning is triggered when [#NONE] is used with follow-on locking
	@Test
	public void testIt(SessionFactoryScope factoryScope, LoggingInspectionsScope loggingScope) {
		factoryScope.inTransaction( (s) -> {
			s.createQuery( "from Item i join i.bids b where name = :name", Object[].class )
					.setParameter( "name", "ZZZZ" )
					.setLockMode( NONE )
					.list();
			Assertions.assertFalse( loggingScope.wereAnyTriggered(), "Log message was unexpectedly triggered" );
		} );
	}

	@Entity(name = "Item")
	@Table(name = "ITEM")
	public static class Item implements Serializable {
		@Id
		String name;

		@Column(name = "i_comment")
		String comment;

		@OneToMany(mappedBy = "item", fetch = FetchType.EAGER)
		Set<Bid> bids = new HashSet<Bid>();
	}

	@Entity(name = "Bid")
	@Table(name = "BID")
	public static class Bid implements Serializable {
		@Id
		float amount;

		@Id
		@ManyToOne
		Item item;

		@Column(name = "b_comment")
		String comment;
	}
}
