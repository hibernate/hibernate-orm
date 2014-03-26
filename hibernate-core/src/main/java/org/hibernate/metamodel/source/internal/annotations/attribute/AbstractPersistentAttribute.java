/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.AccessType;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.logging.Logger;

/**
 * Base class for the different types of persistent attributes
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public abstract class AbstractPersistentAttribute implements PersistentAttribute {
	private static final Logger log = Logger.getLogger( AbstractPersistentAttribute.class );

	private final ManagedTypeMetadata container;
	private final String attributeName;
	private final AttributePath attributePath;
	private final AttributeRole attributeRole;
	private final MemberDescriptor backingMember;
	private final Nature nature;
	private final AccessType accessType;
	private final String accessorStrategy;

	private final boolean includeInOptimisticLocking;

	private final boolean isId;
	private final boolean isVersioned;
	private final NaturalIdMutability naturalIdMutability;

	private final List<Column> columnValues;
	private final FormulaValue formulaValue;

	private final String checkCondition;

	protected AbstractPersistentAttribute(
			ManagedTypeMetadata container,
			String attributeName,
			AttributePath attributePath,
			AttributeRole attributeRole,
			MemberDescriptor backingMember,
			Nature nature,
			AccessType accessType,
			String accessorStrategy) {
		this.container = container;
		this.attributeName = attributeName;
		this.attributePath = attributePath;
		this.attributeRole = attributeRole;
		this.backingMember = backingMember;
		this.accessType = accessType;
		this.accessorStrategy = accessorStrategy;

		this.isId = determineWhetherIsId( backingMember );
		this.isVersioned = determineWhetherIsVersion( backingMember );
		this.naturalIdMutability = determineNaturalIdMutability( container, backingMember );

		// todo : could we just get rid of EMBEDDED_ID as an attribute nature?
		//		Nature.EMBEDDED + isId already covers this case...
		if ( nature == Nature.EMBEDDED ) {
			this.nature = isId ? Nature.EMBEDDED_ID : Nature.EMBEDDED;
		}
		else {
			this.nature = nature;
		}

		this.includeInOptimisticLocking = determineInclusionInOptimisticLocking( backingMember );

		this.columnValues = extractColumnValues( backingMember );
		this.formulaValue = extractFormulaValue( backingMember );

		validateColumnsAndFormulas( columnValues, formulaValue );

		this.checkCondition = extractCheckCondition( backingMember );
	}

	private boolean determineInclusionInOptimisticLocking(MemberDescriptor backingMember) {
		// NOTE : default is `true`, the annotation is used to opt out of inclusion

		final AnnotationInstance optimisticLockAnnotation = backingMember.getAnnotations().get(
				HibernateDotNames.OPTIMISTIC_LOCK
		);
		if ( optimisticLockAnnotation == null ) {
			return true;
		}

		final boolean excludedFromLocking = optimisticLockAnnotation.value( "excluded" ).asBoolean();
		if ( excludedFromLocking ) {
			if ( isId() || isVersion() ) {
				throw getContext().makeMappingException(
						"@OptimisticLock.exclude=true incompatible with @Id, @EmbeddedId and @Version : "
								+ backingMember.toString()
				);
			}
		}
		return !excludedFromLocking;
	}

	protected final boolean hasOptimisticLockAnnotation() {
		return getBackingMember().getAnnotations().get( HibernateDotNames.OPTIMISTIC_LOCK ) != null;
	}

	private String extractCheckCondition(MemberDescriptor backingMember) {
		final AnnotationInstance checkAnnotation = backingMember.getAnnotations().get( HibernateDotNames.CHECK );
		if ( checkAnnotation == null ) {
			return null;
		}

		final AnnotationValue constraintsValue = checkAnnotation.value( "constraints" );
		if ( constraintsValue == null ) {
			return null;
		}

		final String constraintsString = constraintsValue.asString();
		if ( StringHelper.isEmpty( constraintsString ) ) {
			return null;
		}

		return constraintsString;
	}

	@SuppressWarnings("RedundantIfStatement")
	protected boolean determineWhetherIsId(MemberDescriptor backingMember) {
		// if this attribute has either @Id or @EmbeddedId, then it is an id attribute
		final AnnotationInstance idAnnotation = backingMember.getAnnotations().get( JPADotNames.ID );
		if ( idAnnotation != null ) {
			validatePresenceOfIdAnnotation();
			if ( backingMember.getType().getErasedType().findTypeAnnotation( JPADotNames.EMBEDDABLE ) != null ) {
				log.warn(
						"Attribute was annotated with @Id, but attribute type was annotated as @Embeddable; " +
								"did you mean to use @EmbeddedId on the attribute rather than @Id?"
				);
			}
			return true;
		}

		final AnnotationInstance embeddedIdAnnotation =  backingMember.getAnnotations().get( JPADotNames.EMBEDDED_ID );
		if ( embeddedIdAnnotation != null ) {
			validatePresenceOfEmbeddedIdAnnotation();
			return true;
		}

		return false;
	}

	protected void validatePresenceOfIdAnnotation() {
//		throw container.getLocalBindingContext().makeMappingException(
//				"Unexpected presence of @Id annotation : " + backingMember.toLoggableForm()
//		);
	}

	protected void validatePresenceOfEmbeddedIdAnnotation() {
//		throw container.getLocalBindingContext().makeMappingException(
//				"Unexpected presence of @EmbeddedId annotation : " + backingMember.toLoggableForm()
//		);
	}

	protected boolean determineWhetherIsVersion(MemberDescriptor backingMember) {
		final AnnotationInstance versionAnnotation = backingMember.getAnnotations().get( JPADotNames.VERSION );
		if ( versionAnnotation != null ) {
			validatePresenceOfVersionAnnotation();
			return true;
		}

		return false;
	}

	protected void validatePresenceOfVersionAnnotation() {
		throw container.getLocalBindingContext().makeMappingException(
				"Unexpected presence of @Version annotation : " + backingMember.toString()
		);
	}

	protected NaturalIdMutability determineNaturalIdMutability(
			ManagedTypeMetadata container,
			MemberDescriptor backingMember) {
		final NaturalIdMutability result = AnnotationParserHelper.determineNaturalIdMutability( container, backingMember );
		if ( result != NaturalIdMutability.NOT_NATURAL_ID ) {
			validatePresenceOfNaturalIdAnnotation();
		}
		return result;
	}

	protected void validatePresenceOfNaturalIdAnnotation() {
//		throw container.getLocalBindingContext().makeMappingException(
//				"Unexpected presence of @NaturalId annotation : " + backingMember.toString()
//		);
	}

	private List<Column> extractColumnValues(MemberDescriptor backingMember) {
		// @javax.persistence.Column
		final AnnotationInstance columnAnnotation = backingMember.getAnnotations().get( JPADotNames.COLUMN );
		// @org.hibernate.annotations.Columns
		final AnnotationInstance columnsAnnotation = backingMember.getAnnotations().get( HibernateDotNames.COLUMNS );

		if ( columnAnnotation != null && columnsAnnotation != null ) {
			throw getContext().makeMappingException(
					"Should not mix @Column and @Columns annotations on same attribute : " +
							backingMember.toString()
			);
		}

		if ( columnAnnotation == null && columnsAnnotation == null ) {
			// try to avoid unnecessary List creation
			return Collections.emptyList();
		}

		final List<Column> columns = new ArrayList<Column>();
		if ( columnAnnotation != null ) {
			validatePresenceOfColumnAnnotation();
			columns.add( new Column( columnAnnotation ) );
		}
		else {
			validatePresenceOfColumnsAnnotation();
			final AnnotationInstance[] columnAnnotations = JandexHelper.getValue(
					columnsAnnotation,
					"columns",
					AnnotationInstance[].class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class )
			);
			for ( AnnotationInstance annotation : columnAnnotations ) {
				columns.add( new Column( annotation ) );
			}
		}
		return columns;
	}

	protected void validatePresenceOfColumnAnnotation() {
//		throw container.getLocalBindingContext().makeMappingException(
//				"Unexpected presence of @Column annotation : " + backingMember.toString()
//		);
	}

	protected void validatePresenceOfColumnsAnnotation() {
//		throw container.getLocalBindingContext().makeMappingException(
//				"Unexpected presence of @Columns annotation : " + backingMember.toString()
//		);
	}

	private FormulaValue extractFormulaValue(MemberDescriptor backingMember) {
		final AnnotationInstance formulaAnnotation = backingMember.getAnnotations().get( HibernateDotNames.FORMULA );
		if ( formulaAnnotation == null ) {
			return null;
		}

		final String expression = formulaAnnotation.value().asString();
		if ( StringHelper.isEmpty( expression ) ) {
			throw getContext().makeMappingException( "Formula expression cannot be empty string" );
		}

		return new FormulaValue( null, expression );
	}

	private void validateColumnsAndFormulas(List<Column> columnValues, FormulaValue formulaValue) {
		if ( !columnValues.isEmpty() && formulaValue != null ) {
			throw getContext().makeMappingException(
					"Should not mix @Formula and @Column/@Columns annotations : " + backingMember.toString()
			);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// relevant to all attributes

	@Override
	public String getName() {
		return attributeName;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public ManagedTypeMetadata getContainer() {
		return container;
	}

	@Override
	public MemberDescriptor getBackingMember() {
		return backingMember;
	}

	@Override
	public AttributeRole getRole(){
		return attributeRole;
	}

	@Override
	public AttributePath getPath() {
		return attributePath;
	}

	@Override
	public AccessType getAccessType() {
		return accessType;
	}

	@Override
	public String getAccessorStrategy() {
		return accessorStrategy;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// relevant to singular attributes

	public boolean isId() {
		return isId;
	}

	public boolean isVersion() {
		return isVersioned;
	}

	public NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	public FormulaValue getFormulaValue() {
		return formulaValue;
	}

	public List<Column> getColumnValues() {
		return columnValues;
	}

	public EntityBindingContext getContext() {
		return container.getLocalBindingContext();
	}

	@Override
	public boolean isIncludeInOptimisticLocking() {
		return includeInOptimisticLocking;
	}

	public String getCheckCondition() {
		return checkCondition;
	}

	@Override
	public int compareTo(PersistentAttribute mappedProperty) {
		return attributeName.compareTo( mappedProperty.getName() );
	}

	@Override
	public String toString() {
		return "PersistentAttribute{attributeName='" + attributeName + '\'' + '}';
	}

	public abstract boolean isOptional();

	public abstract boolean isInsertable();

	public abstract boolean isUpdatable();

	public abstract PropertyGeneration getPropertyGeneration();

}


