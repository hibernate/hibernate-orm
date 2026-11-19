package org.hibernate.id;

import java.util.List;
import java.util.Properties;

import jakarta.annotation.Nonnull;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GenerationRequests;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.id.IdentifierGeneratorHelper.getForeignId;
import static org.hibernate.id.IdentifierGeneratorHelper.getForeignIds;

/**
 * The legacy id generator named {@code foreign}.
 * <p>
 * An {@code Identifier} generator that uses the value of the id property of an
 * associated object.
 * <p>
 * One mapping parameter is required: {@value PROPERTY}.
 *
 * @author Gavin King
 *
 * @deprecated This remains around as an implementation detail of {@code hbm.xml} mappings.
 */
@Deprecated(since = "6", forRemoval = true)
public class ForeignGenerator implements IdentifierGenerator {

	/**
	 * The parameter which specifies the property holding a reference to the associated object.
	 */
	public static final String PROPERTY = "property";

	private String entityName;
	private String propertyName;
	private Class<?> generatedType;

	/**
	 * Getter for property 'entityName'.
	 *
	 * @return Value for property 'entityName'.
	 */
	public String getEntityName() {
		return entityName;
	}

	/**
	 * Getter for property 'propertyName'.
	 *
	 * @return Value for property 'propertyName'.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Getter for property 'role'.  Role is the {@link #getPropertyName property name} qualified by the
	 * {@link #getEntityName entity name}.
	 *
	 * @return Value for property 'role'.
	 */
	public String getRole() {
		return getEntityName() + '.' + getPropertyName();
	}


	@Override
	public void configure(GeneratorCreationContext creationContext, Properties parameters) throws MappingException {
		propertyName = parameters.getProperty( PROPERTY );
		entityName = parameters.getProperty( ENTITY_NAME );
		generatedType = creationContext.getType().getReturnedClass();
		if ( propertyName==null ) {
			throw new MappingException( "param named \"property\" is required for foreign id generation strategy" );
		}
	}

	@Override
	public Class<?> getGeneratedType() {
		return generatedType;
	}

	@Override
	public Object generate(SharedSessionContractImplementor sessionImplementor, Object object) {
		return getForeignId( entityName, propertyName, sessionImplementor, object );
	}

	@Override
	public boolean supportsBatchGeneration() {
		return true;
	}

	@Override
	public List<BeforeExecutionGenerator> resolveGenerationDependencies(RuntimeModelCreationContext context) {
		final EntityPersister persister = context.getDomainModel().getEntityDescriptor( entityName );
		final var attributeMapping = persister.findAttributeMapping( propertyName );
		final EntityMappingType entityMappingType;
		if ( attributeMapping != null
			&& attributeMapping.getPartMappingType() instanceof EntityMappingType entity ) {
			entityMappingType = entity;
		}
		else {
			final var identifierMapper =
					persister.findAttributeMapping( NavigablePath.IDENTIFIER_MAPPER_PROPERTY );
			assert identifierMapper != null;
			entityMappingType = (EntityMappingType) identifierMapper.asEmbeddedAttributeMapping()
					.getEmbeddableTypeDescriptor()
					.findAttributeMapping( propertyName )
					.getPartMappingType();
		}
		final var referencedGenerator = entityMappingType.getEntityPersister().getGenerator();
		return referencedGenerator instanceof BeforeExecutionGenerator beforeExecutionGenerator
				? List.of( beforeExecutionGenerator )
				: List.of();
	}

	@Override
	public @Nonnull Object[] generateBatch(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull GenerationRequests requests,
			@Nonnull EventType eventType) {
		return getForeignIds( entityName, propertyName, session, requests );
	}

	@Override
	public boolean allowAssignedIdentifiers() {
		return true;
	}
}
