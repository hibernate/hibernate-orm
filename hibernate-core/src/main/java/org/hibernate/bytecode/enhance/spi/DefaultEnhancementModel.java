package org.hibernate.bytecode.enhance.spi;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

/// Annotation-based persistence interpretation using Hibernate's enhancement defaults.
/// Recognizes entities, embeddables, and mapped superclasses by their corresponding
/// Jakarta Persistence annotations, excludes transient fields, and preserves the
/// supplied persistent-field ordering. Subclasses may override individual decisions
/// or [#getCandidates()] to describe an integration's model scope.
///
/// All persistent attributes are eligible for lazy-loading support by default;
/// this preserves the enhancer's existing behavior rather than inferring a fetch
/// plan from annotations. Collection handling recognizes plural mapping annotations
/// and treats otherwise unclassified collection fields as plural unless marked basic.
///
/// This implementation is stateless. Mutable discovery results, including implicit
/// embeddable classifications, belong to the session, not this model.
///
/// @author Steve Ebersole
public class DefaultEnhancementModel implements EnhancementModel {

	@Override
	public boolean isEntityClass(UnloadedClass type) {
		return type.hasAnnotation( Entity.class );
	}

	@Override
	public boolean isCompositeClass(UnloadedClass type) {
		return type.hasAnnotation( Embeddable.class );
	}

	@Override
	public boolean isMappedSuperclassClass(UnloadedClass type) {
		return type.hasAnnotation( MappedSuperclass.class );
	}

	@Override
	public boolean isPersistentField(UnloadedField field) {
		return !field.hasAnnotation( Transient.class );
	}

	@Override
	public UnloadedField[] order(UnloadedField[] fields) {
		return fields;
	}

	@Override
	public boolean hasLazyLoadableAttributes(UnloadedClass type) {
		return true;
	}

	@Override
	public boolean isLazyLoadable(UnloadedField field) {
		return true;
	}

	@Override
	public boolean isMappedCollection(UnloadedField field) {
		return field.hasAnnotation( OneToMany.class ) || field.hasAnnotation( ManyToMany.class )
				|| field.hasAnnotation( ElementCollection.class ) || !field.hasAnnotation( Basic.class );
	}
}
