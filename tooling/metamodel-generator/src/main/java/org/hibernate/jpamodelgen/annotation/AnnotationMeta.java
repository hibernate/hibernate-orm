/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.util.TypeUtils;

import javax.lang.model.element.AnnotationMirror;
import java.util.List;

public abstract class AnnotationMeta implements Metamodel {

	void addAuxiliaryMembers() {
		addAuxiliaryMembersForAnnotation( Constants.NAMED_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.NAMED_NATIVE_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_NATIVE_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.SQL_RESULT_SET_MAPPING, "MAPPING_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.SQL_RESULT_SET_MAPPINGS, "MAPPING_" );
		addAuxiliaryMembersForAnnotation( Constants.NAMED_ENTITY_GRAPH, "GRAPH_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.NAMED_ENTITY_GRAPHS, "GRAPH_" );

		addAuxiliaryMembersForAnnotation( Constants.HIB_NAMED_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_NAMED_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.HIB_NAMED_NATIVE_QUERY, "QUERY_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_NAMED_NATIVE_QUERIES, "QUERY_" );
		addAuxiliaryMembersForAnnotation( Constants.HIB_FETCH_PROFILE, "PROFILE_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_FETCH_PROFILES, "PROFILE_" );
		addAuxiliaryMembersForAnnotation( Constants.HIB_FILTER_DEF, "FILTER_" );
		addAuxiliaryMembersForRepeatableAnnotation( Constants.HIB_FILTER_DEFS, "FILTER_" );
	}

	private void addAuxiliaryMembersForRepeatableAnnotation(String annotationName, String prefix) {
		AnnotationMirror mirror = TypeUtils.getAnnotationMirror( getElement(), annotationName );
		if ( mirror != null ) {
			mirror.getElementValues().forEach((key, value) -> {
				if ( key.getSimpleName().contentEquals("value") ) {
					List<? extends AnnotationMirror> values =
							(List<? extends AnnotationMirror>) value.getValue();
					for ( AnnotationMirror annotationMirror : values ) {
						addAuxiliaryMembersForMirror( annotationMirror, prefix );
					}
				}
			});
		}
	}

	private void addAuxiliaryMembersForAnnotation(String annotationName, String prefix) {
		AnnotationMirror mirror = TypeUtils.getAnnotationMirror( getElement(), annotationName);
		if ( mirror != null ) {
			addAuxiliaryMembersForMirror( mirror, prefix );
		}
	}

	private void addAuxiliaryMembersForMirror(AnnotationMirror mirror, String prefix) {
		mirror.getElementValues().forEach((key, value) -> {
			if ( key.getSimpleName().contentEquals("name") ) {
				String name = value.getValue().toString();
				putMember( prefix + name, new NameMetaAttribute( this, name, prefix ) );
			}
		});
	}

	abstract void putMember(String name, NameMetaAttribute nameMetaAttribute);
}
