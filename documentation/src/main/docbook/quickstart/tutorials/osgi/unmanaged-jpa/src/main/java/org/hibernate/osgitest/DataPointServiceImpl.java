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
package org.hibernate.osgitest;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.osgitest.entity.DataPoint;

/**
 * @author Brett Meyer
 */
public class DataPointServiceImpl implements DataPointService {
	
	private HibernateUtil hibernateUtil = new HibernateUtil();

	public void add(DataPoint dp) {
		EntityManager em = hibernateUtil.getEntityManager();
		em.getTransaction().begin();
		em.persist( dp );
		em.getTransaction().commit();
		em.close();
	}

	public void update(DataPoint dp) {
		EntityManager em = hibernateUtil.getEntityManager();
		em.getTransaction().begin();
		em.merge( dp );
		em.getTransaction().commit();
		em.close();
	}

	public DataPoint get(long id) {
		EntityManager em = hibernateUtil.getEntityManager();
		em.getTransaction().begin();
		DataPoint dp = (DataPoint) em.createQuery( "from DataPoint dp where dp.id=" + id ).getSingleResult();
		em.getTransaction().commit();
		em.close();
		return dp;
	}

	public List<DataPoint> getAll() {
		EntityManager em = hibernateUtil.getEntityManager();
		em.getTransaction().begin();
		List list = em.createQuery( "from DataPoint" ).getResultList();
		em.getTransaction().commit();
		em.close();
		return list;
	}

	public void deleteAll() {
		EntityManager em = hibernateUtil.getEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from DataPoint" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

}
