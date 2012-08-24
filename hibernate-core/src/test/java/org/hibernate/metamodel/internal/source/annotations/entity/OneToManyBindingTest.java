package org.hibernate.metamodel.internal.source.annotations.entity;

import org.junit.Test;

import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.test.annotations.loader.Player;
import org.hibernate.test.annotations.loader.Team;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

import static org.junit.Assert.assertNotNull;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class OneToManyBindingTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = { Team.class, Player.class})
	public void testPluralAttributeBindingTest(){
		EntityBinding playerBinding = getEntityBinding( Player.class );
		assertNotNull( playerBinding );
		for( AttributeBinding attributeBinding : playerBinding.attributeBindings()){
			System.out.println(attributeBinding.getAttribute().getName());
		}
		System.out.println("-----------");
		EntityBinding teamBinding = getEntityBinding( Team.class );
		assertNotNull( teamBinding );
		for(AttributeBinding attributeBinding : teamBinding.attributeBindings()){
			System.out.println(attributeBinding.getAttribute().getName());
		}
	}
}
