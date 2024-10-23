/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Yanming Zhou
 */
@JiraKey( "HHH-18610" )
@DomainModel(annotatedClasses = {
		NativeQueryWithInheritanceTests.SingleTableParent.class,
		NativeQueryWithInheritanceTests.TablePerClassParent.class,
		NativeQueryWithInheritanceTests.JoinedParent.class,
		NativeQueryWithInheritanceTests.SingleTableChild.class,
		NativeQueryWithInheritanceTests.TablePerClassChild.class,
		NativeQueryWithInheritanceTests.JoinedChild.class
})
@SessionFactory
public class NativeQueryWithInheritanceTests {

	@Test
	public void testSingleTable(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNativeQuery("select {p.*} from SingleTableParent p")
					.addEntity( "p", SingleTableParent.class ).list();
		} );
	}

	@Test
	public void testTablePerClass(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNativeQuery("select {p.*} from TablePerClassParent p")
					.addEntity( "p", TablePerClassParent.class ).list();
		} );
	}

	@Test
	public void testJoined(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNativeQuery("select {p.*} from JoinedParent p")
					.addEntity( "p", JoinedParent.class ).list();
		} );
	}

	@Entity(name = "SingleTableParent")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	static class SingleTableParent {
		@Id
		Long id;
	}

	@Entity(name = "SingleTableChild")
	static class SingleTableChild extends SingleTableParent {
	}

	@Entity(name = "TablePerClassParent")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static class TablePerClassParent {
		@Id
		Long id;
	}

	@Entity(name = "TablePerClassChild")
	static class TablePerClassChild extends TablePerClassParent {
	}

	@Entity(name = "JoinedParent")
	@Inheritance(strategy = InheritanceType.JOINED)
	static class JoinedParent {
		@Id
		Long id;
	}

	@Entity(name = "JoinedChild")
	static class JoinedChild extends JoinedParent {
	}
}
