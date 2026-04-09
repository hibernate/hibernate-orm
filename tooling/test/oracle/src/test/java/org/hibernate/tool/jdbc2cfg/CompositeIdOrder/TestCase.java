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
package org.hibernate.tool.jdbc2cfg.CompositeIdOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLException;
import java.util.Iterator;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tools.test.util.HibernateUtil;
import org.hibernate.tools.test.util.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author max
 * @author koen
 */
public class TestCase {
	
	private Metadata metadata = null;
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}
	
	@Test
	public void testMultiColumnForeignKeys() throws SQLException {

		Table table = HibernateUtil.getTable(metadata, "COURSE");
        assertNotNull(table);
        ForeignKey foreignKey = HibernateUtil.getForeignKey(table, "FK_COURSE__SCHEDULE");     
        assertNotNull(foreignKey);
                
        assertEquals("Schedule", foreignKey.getReferencedEntityName() );
        assertEquals("COURSE", foreignKey.getTable().getName() );
        
        assertEquals(1,foreignKey.getColumnSpan() );
        assertEquals(foreignKey.getColumn(0).getName(), "SCHEDULE_KEY");
        
        assertEquals(table.getPrimaryKey().getColumn(0).getName(), "SCHEDULE_KEY");
        assertEquals(table.getPrimaryKey().getColumn(1).getName(), "REQUEST_KEY");
        
        PersistentClass course = metadata.getEntityBinding("Course");
        
        assertEquals(2,course.getIdentifier().getColumnSpan() );
        Iterator<Selectable> selectablesIterator = course.getIdentifier().getSelectables().iterator();
        assertEquals(((Column)(selectablesIterator.next())).getName(), "SCHEDULE_KEY");
        assertEquals(((Column)(selectablesIterator.next())).getName(), "REQUEST_KEY");
        
        PersistentClass topic = metadata.getEntityBinding("CourseTopic");
        
        Property property = topic.getProperty("course");
        selectablesIterator = property.getValue().getSelectables().iterator();
        assertEquals(((Column)(selectablesIterator.next())).getName(), "SCHEDULE_KEY");
        assertEquals(((Column)(selectablesIterator.next())).getName(), "REQUEST_KEY");

	}


}
