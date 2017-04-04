/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for HHH-10106
 *
 * @author Vyacheslav Rarata
 */
public class CollectionBinderTest extends BaseUnitTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-10106")
	public void testAssociatedClassException() throws SQLException {
		final Collection collection = mock(Collection.class);
		final Map persistentClasses = mock(Map.class);
		final XClass collectionType = mock(XClass.class);
		final MetadataBuildingContext buildingContext = mock(MetadataBuildingContext.class);
		final InFlightMetadataCollector inFly = mock(InFlightMetadataCollector.class);
		final PersistentClass persistentClass = mock(PersistentClass.class);
		final Table table = mock(Table.class);
		
		when(buildingContext.getMetadataCollector()).thenReturn(inFly);
		when(persistentClasses.get(null)).thenReturn(null);
		when(collection.getOwner()).thenReturn(persistentClass);
		when(collectionType.getName()).thenReturn("List");
		when(persistentClass.getTable()).thenReturn(table);
		when(table.getName()).thenReturn("Hibernate");
		
		CollectionBinder collectionBinder = new CollectionBinder(false) {
			@Override
			protected Collection createCollection(PersistentClass persistentClass) {
				return null;
			}

			{
				final PropertyHolder propertyHolder = Mockito.mock(PropertyHolder.class);
				when(propertyHolder.getClassName()).thenReturn( CollectionBinderTest.class.getSimpleName() );
				this.propertyName = "abc";
				this.propertyHolder = propertyHolder;
			}
		};

		String expectMessage = "Association [abc] for entity [CollectionBinderTest] references unmapped class [List]";
		try {
			collectionBinder.bindOneToManySecondPass(collection, persistentClasses, null, collectionType, false, false, buildingContext, null);
		} catch (MappingException e) {
			assertEquals(expectMessage, e.getMessage());
		}
	}

}
