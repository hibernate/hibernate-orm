package org.hibernate.orm.test.collection.map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;

/// The map key is mapped with `<map-key-formula>` in `orm.xml`, which is
/// `metadata-complete`, so the annotations of the entities are ignored.
@DomainModel(xmlMappings = "org/hibernate/orm/test/collection/map/MapKeyFormula.xml")
@SessionFactory
@JiraKey( "HHH-21053" )
public class MapKeyFormulaXmlTest extends AbstractMapKeyFormulaTest {
}
