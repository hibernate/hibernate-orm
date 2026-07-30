package org.hibernate.boot.mapping.internal.categorize;


/// Standard AggregatedKeyMapping impl
///
/// @since 9.0
/// @author Steve Ebersole
public record AggregatedKeyMappingImpl(AttributeMetadataImplementor attribute) implements AggregatedKeyMapping {
	@Override
	public AttributeMetadataImplementor getAttribute() {
		return attribute;
	}
}
