package org.hibernate.metamodel.mapping.ordering.ast;

import jakarta.annotation.Nullable;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Represents a domain-path (model part path) used in an order-by fragment
 *
 * @author Steve Ebersole
 */
public interface DomainPath extends OrderingExpression, SequencePart {
	NavigablePath getNavigablePath();

	@Nullable
	DomainPath getLhs();

	ModelPart getReferenceModelPart();

	default PluralAttributeMapping getPluralAttribute() {
		return castNonNull( getLhs() ).getPluralAttribute();
	}

	@Override
	default String toDescriptiveText() {
		return "domain path (" + getNavigablePath().getFullPath() + ")";
	}
}
