/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Allows the BaseCoreFunctionalTestCase to create the schema using TestEntity.  The test method validates against an
 * identical entity, but using the synonym name.
 * 
 * @author Brett Meyer
 */
@RequiresDialect( Oracle9iDialect.class )
public class SynonymValidationTest extends BaseNonConfigCoreFunctionalTestCase {
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}
	
	@Test
	public void testSynonymValidation() {
		Session s = openSession();
		s.getTransaction().begin();
		s.createSQLQuery( "CREATE SYNONYM test_synonym FOR test_entity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
		
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( TestEntityWithSynonym.class );
		cfg.setProperty( AvailableSettings.ENABLE_SYNONYMS, "true" );
		
		SchemaValidator schemaValidator = new SchemaValidator( metadata() );
		schemaValidator.validate();
		
		s = openSession();
		s.getTransaction().begin();
		s.createSQLQuery( "DROP SYNONYM test_synonym FORCE" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
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
