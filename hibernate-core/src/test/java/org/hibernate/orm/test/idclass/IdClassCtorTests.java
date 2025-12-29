/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {IdClassCtorTests.SystemUser.class, IdClassCtorTests.SystemUserId.class})
@SessionFactory
public class IdClassCtorTests {
	@Test
	void testIdClassRecordUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.find( SystemUser.class, new SystemUserId( "payroll", "admin" ) );
			session.createQuery( "from SystemUser u where u.privileges % 2 = 0" ).list();
		} );
	}

	@Entity(name="SystemUser")
	@Table(name="SystemUser")
	@IdClass( SystemUserId.class )
	public static class SystemUser {
		@Id
		private String systemName;
		@Id
		private String userName;
		@Column(name = "privs")
		private int privileges;
	}

	public record SystemUserId(String systemName, String userName) {
	}
}
