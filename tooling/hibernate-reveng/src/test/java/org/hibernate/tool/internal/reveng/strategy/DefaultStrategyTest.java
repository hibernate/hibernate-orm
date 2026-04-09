/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.tool.internal.reveng.strategy;

import org.hibernate.mapping.Column;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author max
 * @author koen
 *
 */
public class DefaultStrategyTest {
	
	RevengStrategy rns = new DefaultStrategy();
	
	@Test
	public void testColumnKeepCase() {
		assertEquals("name", rns.columnToPropertyName(null, "name") );		
		assertEquals("nameIsValid", rns.columnToPropertyName(null, "nameIsValid") );
	}
	
	@Test
	public void testColumnUpperToLower() {
		assertEquals("name", rns.columnToPropertyName(null, "NAME") );
		assertEquals("name", rns.columnToPropertyName(null, "Name") );
	}
	
	@Test
	public void testColumnRemoveChars() {
		assertEquals("name", rns.columnToPropertyName(null, "_Name") );
		assertEquals("name", rns.columnToPropertyName(null, "_name") );
		assertEquals("name", rns.columnToPropertyName(null, "_name") );
	}
	
	@Test
	public void testColumnToCamelCase() {
		assertEquals("labelForField", rns.columnToPropertyName(null, "LABEL_FOR_FIELD") );
		assertEquals("nameToMe", rns.columnToPropertyName(null, "_name-To-Me") );
	}
	
	@Test
	public void testColumnChangeCamelCase() {
		assertEquals("labelForField", rns.columnToPropertyName(null, "LabelForField") );	
	}
	
	@Test
	public void testTableKeepCase() {
		assertEquals("SickPatients", rns.tableToClassName(TableIdentifier.create(null, null, "SickPatients") ) );
	}
	
	@Test
	public void testTableUpperToLower() {
		assertEquals("Patients", rns.tableToClassName(TableIdentifier.create(null, null, "PATIENTS") ) );
		assertEquals("Patients", rns.tableToClassName(TableIdentifier.create(null, null, "patients") ) );
	}
	
	@Test
	public void testTableRemoveChars() {
		assertEquals("Patients", rns.tableToClassName(TableIdentifier.create(null, null, "_Patients") ) );
		assertEquals("Patients", rns.tableToClassName(TableIdentifier.create(null, null, "_patients") ) );
		assertEquals("Patients", rns.tableToClassName(TableIdentifier.create(null, null, "_patients") ) );
		assertEquals("PatientInterventions", rns.tableToClassName(TableIdentifier.create(null, null, "_PATIENT_INTERVENTIONS") ) );
	}
	
	@Test
	public void testTableToCamelCase() {
		assertEquals("SickPatients", rns.tableToClassName(TableIdentifier.create(null, null, "Sick_Patients") ) );
		assertEquals("SickPatients", rns.tableToClassName(TableIdentifier.create(null, null, "_Sick-Patients") ) );
	}
	
	@Test
	public void testTableKeepCamelCase() {
		assertEquals("SickPatients", rns.tableToClassName(TableIdentifier.create(null, null, "SickPatients") ) );
	}
    
	@Test
    public void testBasicForeignKeyNames() {
        assertEquals("products", rns.foreignKeyToCollectionName("something", TableIdentifier.create(null, null, "product"), null, TableIdentifier.create(null, null, "order"), null, true ) );
        assertEquals("willies", rns.foreignKeyToCollectionName("something", TableIdentifier.create(null, null, "willy"), null, TableIdentifier.create(null, null, "order"), null, true ) );
		assertEquals("boxes", rns.foreignKeyToCollectionName("something", TableIdentifier.create(null, null, "box"), null, TableIdentifier.create(null, null, "order"), null, true ) );
        assertEquals("order", rns.foreignKeyToEntityName("something", TableIdentifier.create(null, null, "product"), null, TableIdentifier.create(null, null, "order"), null, true ) );
    }
	
	@Test
    public void testCustomClassNameStrategyWithCollectionName() {
    	
    	RevengStrategy custom = new DelegatingStrategy(new DefaultStrategy()) {
    		public String tableToClassName(TableIdentifier tableIdentifier) {
    			return super.tableToClassName( tableIdentifier ) + "Impl";
    		}
    	};

    	custom.setSettings( new RevengSettings(custom) );
    	
    	TableIdentifier productTable = TableIdentifier.create(null, null, "product");
		assertEquals("ProductImpl", custom.tableToClassName( productTable ));
    	
        assertEquals("productImpls", custom.foreignKeyToCollectionName("something", productTable, null, TableIdentifier.create(null, null, "order"), null, true ) );
    }
    
	@Test
    public void testForeignKeyNamesToPropertyNames() {
    	
    	String fkName = "something";
		TableIdentifier fromTable = TableIdentifier.create(null, null, "company");
		List<Column> fromColumns = new ArrayList<>();
		
		TableIdentifier toTable = TableIdentifier.create(null, null, "address");
		List<Column> toColumns = new ArrayList<>();
		
		assertEquals("address", rns.foreignKeyToEntityName(fkName, fromTable, fromColumns, toTable, toColumns, true) );
		assertEquals("companies", rns.foreignKeyToCollectionName(fkName, fromTable, fromColumns, toTable, toColumns, true) );
		
		fkName = "billing";
		fromColumns.clear();		
		fromColumns.add(new Column("bill_adr") );
		assertEquals("addressByBillAdr", rns.foreignKeyToEntityName(fkName, fromTable, fromColumns, toTable, toColumns, false) );
		assertEquals("companiesForBillAdr", rns.foreignKeyToCollectionName(fkName, fromTable, fromColumns, toTable, toColumns, false) );
		
		fromColumns.add(new Column("bill_adrtype") );
		assertEquals("addressByBilling", rns.foreignKeyToEntityName(fkName, fromTable, fromColumns, toTable, toColumns, false) );
		assertEquals("companiesForBilling", rns.foreignKeyToCollectionName(fkName, fromTable, fromColumns, toTable, toColumns, false) );
    }

	@Test
    public void testPreferredTypes() {
    	assertEquals("int",rns.columnToHibernateTypeName(null, "bogus",Types.INTEGER,0,0,0, false, false));
    	assertEquals("java.lang.Integer",rns.columnToHibernateTypeName(null, "bogus",Types.INTEGER,0,0,0, true, false), "because nullable it should not be int");
    	assertEquals("java.lang.Integer",rns.columnToHibernateTypeName(null, "bogus",Types.NUMERIC,0,9,0, true, false));
       	assertEquals("java.lang.Integer",rns.columnToHibernateTypeName(null, "bogus",Types.INTEGER,0,0,0, true, false));			
       	assertEquals("serializable",rns.columnToHibernateTypeName(TableIdentifier.create(null, null, "sdf"), "bogus",-567,0,0,0, false, false));
       	
       	assertEquals("string",rns.columnToHibernateTypeName(TableIdentifier.create(null, null, "sdf"), "bogus",12,0,0,0, false, false));
    }
    
	@Test
    public void testReservedKeywordsHandling() {
    	assertEquals("class_", rns.columnToPropertyName(TableIdentifier.create(null, null, "blah"), "class"));    	
    }
     
}
