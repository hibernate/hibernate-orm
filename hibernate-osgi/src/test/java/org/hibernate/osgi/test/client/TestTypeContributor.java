/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.osgi.test.client;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.usertype.UserType;


/**
 * @author Brett Meyer
 */
public class TestTypeContributor implements TypeContributor {

	public static final String NAME = "test-type";

	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeType( new TestType(), NAME );
	}

	public static class TestType implements UserType {
		@Override
		public int[] sqlTypes() {
			return new int[0];
		}

		@Override
		public Class returnedClass() {
			return null;
		}

		@Override
		public boolean equals(Object x, Object y) throws HibernateException {
			return false;
		}

		@Override
		public int hashCode(Object x) throws HibernateException {
			return 0;
		}

		@Override
		public Object nullSafeGet(
				ResultSet rs, String[] names, SessionImplementor session, Object owner)
				throws HibernateException, SQLException {
			return null;
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st, Object value, int index, SessionImplementor session)
				throws HibernateException, SQLException {

		}

		@Override
		public Object deepCopy(Object value) throws HibernateException {
			return null;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Object value) throws HibernateException {
			return null;
		}

		@Override
		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return null;
		}

		@Override
		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return null;
		}
	}
}
