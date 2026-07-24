package org.hibernate.boot.mapping.internal.categorize;


/// Standard AggregatedKeyMapping impl
///
/// @since 9.0
/// @author Steve Ebersole
public record AggregatedKeyMappingImpl(AttributeMetadata attribute) implements AggregatedKeyMapping {
	@Override
	public AttributeMetadata getAttribute() {
		return attribute;
	}
}
