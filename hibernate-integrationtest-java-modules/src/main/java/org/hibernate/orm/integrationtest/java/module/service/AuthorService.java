/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.integrationtest.java.module.service;

import java.util.List;

import javax.persistence.Persistence;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.integrationtest.java.module.entity.Author;

public class AuthorService implements AutoCloseable {

	private final SessionFactory sessionFactory;

	public AuthorService() {
		sessionFactory = createSessionFactory();
	}

	private SessionFactory createSessionFactory() {
		return Persistence.createEntityManagerFactory( "primaryPU" )
				.unwrap( SessionFactory.class );
	}

	public void add(String name, Integer favoriteNumber) {
		try ( Session session = sessionFactory.openSession() ) {
			session.getTransaction().begin();
			try {
				Author entity = new Author();
				entity.setName( name );
				entity.setFavoriteNumber( favoriteNumber );
				session.save( entity );
				session.getTransaction().commit();
			}
			catch (Throwable e) {
				try {
					session.getTransaction().rollback();
				}
				catch (Throwable e2) {
					e.addSuppressed( e2 );
				}
				throw e;
			}
		}
	}

	public void update(String name, Integer favoriteNumber) {
		try ( Session session = sessionFactory.openSession() ) {
			session.getTransaction().begin();
			try {
				Author entity = session.bySimpleNaturalId( Author.class ).getReference( name );
				entity.setFavoriteNumber( favoriteNumber );
				session.getTransaction().commit();
			}
			catch (Throwable e) {
				try {
					session.getTransaction().rollback();
				}
				catch (Throwable e2) {
					e.addSuppressed( e2 );
				}
				throw e;
			}
		}
	}

	public Integer getFavoriteNumber(String name) {
		try ( Session session = sessionFactory.openSession() ) {
			session.getTransaction().begin();
			try {
				Author entity = session.bySimpleNaturalId( Author.class ).getReference( name );
				Integer result = entity.getFavoriteNumber();
				session.getTransaction().rollback();
				return result;
			}
			catch (Throwable e) {
				try {
					session.getTransaction().rollback();
				}
				catch (Throwable e2) {
					e.addSuppressed( e2 );
				}
				throw e;
			}
		}
	}

	public List<Number> getRevisions(String name) {
		try ( Session session = sessionFactory.openSession() ) {
			Author entity = session.bySimpleNaturalId( Author.class ).getReference( name );
			AuditReader auditReader = AuditReaderFactory.get( session );
			return auditReader.getRevisions( Author.class, entity.getId() );
		}
	}

	@Override
	public void close() {
		sessionFactory.close();
	}
}
