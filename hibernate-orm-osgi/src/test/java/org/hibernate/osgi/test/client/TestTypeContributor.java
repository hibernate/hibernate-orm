/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi.test.client;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
				ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
				throws HibernateException, SQLException {
			return null;
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
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
