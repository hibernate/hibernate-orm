/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal.jandex;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.jaxb.JaxbGeneratedValue;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmIdGenerator;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmParam;
import org.hibernate.metamodel.source.internal.jaxb.JaxbId;
import org.hibernate.metamodel.source.internal.jaxb.PersistentAttribute;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class IdMocker extends PropertyMocker {
	private final JaxbId id;

	IdMocker(IndexBuilder indexBuilder, ClassInfo classInfo, Default defaults, JaxbId id) {
		super( indexBuilder, classInfo, defaults );
		this.id = id;
	}

	@Override
	protected PersistentAttribute getPersistentAttribute() {
		return id;
	}

	@Override
	protected void doProcess() {
		create( ID );
		parseColumn( id.getColumn(), getTarget() );
		parseGeneratedValue( id.getGeneratedValue(), getTarget() );
		parseGenerator( id.getGenerator(), getTarget() );
		parseTemporalType( id.getTemporal(), getTarget() );
	}

	private void parseGeneratedValue(JaxbGeneratedValue generatedValue, AnnotationTarget target) {
		if ( generatedValue == null ) {
			return;
		}
		
		// @GeneratedValue(generator = "...")
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.enumValue( "strategy", GENERATION_TYPE, generatedValue.getStrategy(), annotationValueList );
		MockHelper.stringValue( "generator", generatedValue.getGenerator(), annotationValueList );
		create( GENERATED_VALUE, target, annotationValueList );
		
		// TODO: Assumes all generators are generic.  How to check to see if it's custom?
		if (! StringHelper.isEmpty( generatedValue.getGenerator() )) {
			// @GenericGenerator(name = "...", strategy = "...")
			annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.stringValue( "name", generatedValue.getGenerator(), annotationValueList );
			MockHelper.stringValue( "strategy", generatedValue.getGenerator(), annotationValueList );
			create( GENERIC_GENERATOR, target, annotationValueList );
		}
	}

	private void parseGenerator(JaxbHbmIdGenerator generator, AnnotationTarget target) {
		if ( generator == null ) {
			return;
		}
		
		// TODO: Is it a safe assumption that this always needs generated?
		final String generatorName = getTargetName() + "." + id.getName() + ".IdGen";
		
		// @GeneratedValue(generator = "...")
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "generator", generatorName, annotationValueList );
		create( GENERATED_VALUE, target, annotationValueList );
		
		// @GenericGenerator(name = "...", strategy = "...")
		annotationValueList = new ArrayList<AnnotationValue>();
		// just generate one
		MockHelper.stringValue( "name", generatorName, annotationValueList );
		MockHelper.stringValue( "strategy", generator.getStrategy(), annotationValueList );
		nestedParamList( "parameters", generator.getParam(), annotationValueList );
		create( GENERIC_GENERATOR, target, annotationValueList );
	}
	
	protected void nestedParamList(String name, List<JaxbHbmParam> params, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( params ) ) {
			AnnotationValue[] values = new AnnotationValue[params.size()];
			for ( int i = 0; i < params.size(); i++ ) {
				AnnotationInstance annotationInstance = parseParam( params.get( i ), null );
				values[i] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			MockHelper.addToCollectionIfNotNull( annotationValueList, AnnotationValue.createArrayValue( name, values ) );
		}

	}

	// @Index
	protected AnnotationInstance parseParam(JaxbHbmParam param, AnnotationTarget target) {
		if ( param == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", param.getName(), annotationValueList );
		MockHelper.stringValue( "value", param.getValue(), annotationValueList );
		return create( PARAMETER, target, annotationValueList );
	}
}
