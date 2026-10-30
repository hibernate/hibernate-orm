package org.hibernate.orm.test.collection.map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;

/// The map key is mapped with [org.hibernate.annotations.MapKeyFormula].
@DomainModel(annotatedClasses = {AbstractMapKeyFormulaTest.Parent.class, AbstractMapKeyFormulaTest.Child.class})
@SessionFactory
@JiraKey( "HHH-21053" )
public class MapKeyFormulaAnnotationTest extends AbstractMapKeyFormulaTest {
}
