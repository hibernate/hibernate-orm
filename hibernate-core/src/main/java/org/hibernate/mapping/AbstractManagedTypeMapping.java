/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractManagedTypeMapping implements ManagedTypeMapping {
	private static final Logger log = Logger.getLogger( AbstractManagedTypeMapping.class );

	private AbstractManagedTypeMapping superclassMapping;
	private List<ManagedTypeMapping> subclassMappings;

	private TreeMap<String,Property> declaredPropertyMap;

	protected void injectSuperclassMapping(AbstractManagedTypeMapping superclassMapping) {
		if ( this.superclassMapping != null ) {
			log.debugf( "ManagedTypeMapping#injectSuperclassMapping called multiple times" );
		}

		this.superclassMapping = superclassMapping;
		superclassMapping.addSubclass( this );
	}

	protected void addSubclass(AbstractManagedTypeMapping subclassMapping) {
		if ( subclassMappings == null ) {
			subclassMappings = new ArrayList<>();
		}
		subclassMappings.add( subclassMapping );
	}

	@Override
	public ManagedTypeMapping getSuperclassMapping() {
		return superclassMapping;
	}

	@Override
	public List<ManagedTypeMapping> getSubclassMappings() {
		return subclassMappings == null ? Collections.emptyList() : Collections.unmodifiableList( subclassMappings );
	}

	@Override
	public List<Property> getDeclaredProperties() {
		// todo (6.0) : think through exposing these in alphabetic order.
		// 		consistent and determinable for external stuff (e.g. bytecode enhancement keeping
		// 		stuff as arrays rather than Maps (perf) to report back to runtime model.
		//
		//		allow control over ordering in generated SQL?  E.g.:
		//
		//			@Entity
		//			class Person {
		//				@ColumnInsertPosition(2)
		//				private String name;
		//				@ColumnInsertPosition(1)
		//				private String uid;
		//				...
		//			}
		//
		//		^^ would indicate that the column for `uid` would come before the column for `name`
		//		^^ ids and versions et.al. reported separately and "external stuff" would have to:
		//			1) understand that, and
		//			2) understand how to find it (annotations *and* XML, not trivial) and
		//				interpret it
		//
		//		^^ also, how would hierarchies play into this?

		if ( declaredPropertyMap == null ) {
			return Collections.emptyList();
		}

		final ArrayList<Property> declaredProperties = new ArrayList<>();
		// NOTE : TreeMap also orders `#values` iteration based on the Map's key (the attribute name)
		for ( Property property : declaredPropertyMap.values() ) {
			declaredProperties.add( property );
		}
		return declaredProperties;
	}

	protected void addDeclaredProperty(Property property) {
		if ( declaredPropertyMap == null ) {
			declaredPropertyMap = new TreeMap<>( String::compareTo );
		}

		declaredPropertyMap.put( property.getName(), property );
	}
}
