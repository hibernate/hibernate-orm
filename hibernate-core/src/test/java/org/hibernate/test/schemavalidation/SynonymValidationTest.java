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
package org.hibernate.test.schemavalidation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.dialect.Oracle9iDialect;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Allows the BaseCoreFunctionalTestCase to create the schema using TestEntity.  The test method validates against an
 * identical entity, but using the synonym name.
 * 
 * @author Brett Meyer
 */
@RequiresDialect( Oracle9iDialect.class )
public class SynonymValidationTest extends BaseCoreFunctionalTestCase {
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testSynonymValidation() {
		fail( "Needs to be implemented for new SchemaValidator" );
//
//		Session s = openSession();
//		s.getTransaction().begin();
//		s.createSQLQuery( "CREATE SYNONYM test_synonym FOR test_entity" ).executeUpdate();
//		s.getTransaction().commit();
//		s.close();
//
//		Configuration cfg = new Configuration();
//		cfg.addAnnotatedClass( TestEntityWithSynonym.class );
//		cfg.setProperty( AvailableSettings.ENABLE_SYNONYMS, "true" );
//
//		SchemaValidator schemaValidator = new SchemaValidator( cfg );
//		schemaValidator.validate();
//
//		s = openSession();
//		s.getTransaction().begin();
//		s.createSQLQuery( "DROP SYNONYM test_synonym FORCE" ).executeUpdate();
//		s.getTransaction().commit();
//		s.close();
	}
	
	@Entity
	@Table(name = "test_entity")
	private static class TestEntity {
	    @Id
	    @GeneratedValue
	    private Long id;
	    
	    @Column(nullable = false)
	    private String key;
	    
	    private String value;

	    public Long getId() {
	        return id;
	    }

	    public void setId(Long id) {
	        this.id = id;
	    }

	    public String getKey() {
	        return key;
	    }

	    public void setKey(String key) {
	        this.key = key;
	    }

	    public String getValue() {
	        return value;
	    }

	    public void setValue(String value) {
	        this.value = value;
	    }
	}
	
	@Entity
	@Table(name = "test_synonym")
	private static class TestEntityWithSynonym {
	    @Id
	    @GeneratedValue
	    private Long id;
	    
	    @Column(nullable = false)
	    private String key;
	    
	    private String value;

	    public Long getId() {
	        return id;
	    }

	    public void setId(Long id) {
	        this.id = id;
	    }

	    public String getKey() {
	        return key;
	    }

	    public void setKey(String key) {
	        this.key = key;
	    }

	    public String getValue() {
	        return value;
	    }

	    public void setValue(String value) {
	        this.value = value;
	    }
	}
}
