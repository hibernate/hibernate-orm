/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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



/**
 * OSGi service interface providing multiple uses of Native and JPA functionality.  The use of a SF/EMF must occur in
 * this separate bundle, rather than attempting to programmatically create a bundle and obtain/use an SF/EMF there.
 * See comments on OsgiTestCase
 * 
 * @author Brett Meyer
 */
public interface TestService {
	public void saveJpa(DataPoint dp);
	
	public DataPoint getJpa(long id);
	
	public void updateJpa(DataPoint dp);
	
	public void deleteJpa();
	
	public void saveNative(DataPoint dp);
	
	public DataPoint getNative(long id);
	
	public void updateNative(DataPoint dp);
	
	public void deleteNative();
	
	public DataPoint lazyLoad(long id);
	
	public TestIntegrator getTestIntegrator();
	
	public TestStrategyRegistrationProvider getTestStrategyRegistrationProvider();
	
	public TestTypeContributor getTestTypeContributor();
}
