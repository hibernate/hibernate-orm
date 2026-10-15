package org.hibernate.orm.test.onetoone.formula.inference;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.MappingException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.ToOne;
import org.hibernate.type.SpecialOneToOneType;
import org.hibernate.testing.orm.junit.DialectContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies source-column inference independently of target-reference binding.
///
/// @author Steve Ebersole
public class SharedPrimaryKeyInferenceTest {
	private static final String PREFIX = SharedPrimaryKeyInferenceTest.class.getName() + "$";

	@ParameterizedTest
	@ValueSource(strings = { "", "true,true", "true,false", "false,true", "false,false" })
	void xmlWriteFlags(String flags) {
		withXml( model( "8.0", simpleOwner( flags, "id", "", "" ), target(), "" ), false,
				metadata -> assertSpecial( metadata, XmlOwner.class ) );
	}

	@Test
	void xml70RetainsAssociationInsertWriter() {
		withXml( model( "7.0", simpleOwner( "true,true", "id", "", "<property-ref name=\"key\"/>" ), target().replace( "<attributes>", "<table><unique-constraint><column-name>personName</column-name><column-name>addressType</column-name></unique-constraint></table><attributes>" ), "" ), false,
				metadata -> {
					var value = association( metadata, XmlOwner.class );
					assertThat( value ).isInstanceOf( org.hibernate.mapping.ManyToOne.class );
					assertThat( value.getColumnInsertability() ).containsExactly( true, false );
					assertThat( value.getColumnUpdateability() ).containsExactly( true, false );
				} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "7.0", "8.0" })
	void associationInsertWriterMigration(String version) {
		boolean legacy = version.equals( "7.0" );
		String owner = simpleOwner( "", "id", "", "<property-ref name=\"lookup\"/>" );
		if ( !legacy ) {
			owner = owner.replace( "<column name=\"id\" insertable=\"false\" updatable=\"false\"/>", "<column name=\"id\"/>" );
		}
		String target = """
				<entity class="%sXmlTarget" access="FIELD" metadata-complete="true">
				<table><unique-constraint><column-name>personName</column-name><column-name>addressType</column-name></unique-constraint></table>
				<attributes><embedded-id name="key">
				<attribute-override name="id"><column name="primaryId"/></attribute-override>
				<attribute-override name="kind"><column name="primaryKind"/></attribute-override>
				</embedded-id><embedded name="lookup"/></attributes></entity>
				""".formatted( PREFIX );
		withXml( model( version, owner, target, "" ), false, metadata -> {
			try ( var factory = metadata.buildSessionFactory() ) {
				factory.inTransaction( session -> {
					XmlTarget address = new XmlTarget();
					address.key = new Key();
					address.key.id = 77L;
					address.lookup = new Key();
					address.lookup.id = 42L;
					address.lookup.kind = "HOME";
					address.key.kind = "HOME";
					session.persist( address );
					XmlOwner person = new XmlOwner();
					person.id = 42L;
					person.target = address;
					session.persist( person );
				} );
				factory.inTransaction( session -> assertThat( session.find( XmlOwner.class, 42L ).target.key.id ).isEqualTo( 77L ) );
			}
		} );
	}

	@Test
	void nonInsertableIdentifierDoesNotDisableInference() {
		withXml( model( "8.0", simpleOwner( "", "id", "", "" ), target(), "" ), false,
				metadata -> {
					assertSpecial( metadata, XmlOwner.class );
					assertThat( metadata.getEntityBinding( XmlOwner.class.getName() )
							.getIdentifier().getColumnInsertability() ).containsOnly( false );
				} );
	}

	@Test
	void logicalPhysicalNaming() {
		withXml( model( "8.0", simpleOwner( "", "ownerId", "", "" ), target(), "" ), true,
				metadata -> assertSpecial( metadata, XmlOwner.class ) );
	}

	@Test
	void quotedColumnIdentity() {
		withXml( model( "8.0", simpleOwner( "", "&quot;OwnerId&quot;", "", "" ), target(), "" ), false,
				metadata -> assertSpecial( metadata, XmlOwner.class ) );
	}

	@Test
	void targetPrimaryKeyDoesNotMakeOtherSourceColumnsReadOnly() {
		String owner = simpleOwner( "true,false", "id", "", "" )
				.replace( "<join-column name=\"id\"", "<join-column name=\"other\"" );
		withXml( model( "8.0", owner, target(), "" ), false, metadata -> {
			var value = association( metadata, XmlOwner.class );
			assertThat( value ).isInstanceOf( org.hibernate.mapping.ManyToOne.class );
			assertThat( value.getColumnInsertability() ).containsExactly( true, false );
			assertThat( value.getColumnUpdateability() ).containsOnly( false );
		} );
	}

	@Test
	void sameNamedSecondaryTableKeyIsNotOwnerIdentifier() {
		String owner = simpleOwner( "true,true", "id", "", "" )
				.replace( "<attributes>", "<secondary-table name=\"owner_extra\"><primary-key-join-column name=\"id\"/></secondary-table><attributes>" )
				.replace( "<join-column name=\"id\"", "<join-column table=\"owner_extra\" name=\"id\"" );
		var exception = assertThrows( MappingException.class, () -> withXml( model( "8.0", owner, target(), "" ), false, metadata -> {
			var value = association( metadata, XmlOwner.class );
			assertThat( value ).isInstanceOf( org.hibernate.mapping.ManyToOne.class );
			assertThat( value.getTable().getName() ).isEqualTo( "owner_extra" );
			assertThat( value.getColumnInsertability() ).containsExactly( true, false );
		} ) );
		assertThat( exception.getMessage() ).contains( "duplicated", "id" );
	}

	@Test
	void embeddedIdentifierSubset() {
		String owner = """
				<entity class="%sEmbeddedOwner" access="FIELD" metadata-complete="true">
				<attributes><embedded-id name="key"/>
					<one-to-one name="target" target-entity="%sXmlTarget">
					<join-column name="personName"/><join-formula>'HOME'</join-formula>
					</one-to-one>
				</attributes>
				</entity>
				""".formatted( PREFIX, PREFIX );
		withXml( model( "8.0", owner, target(), "" ), false,
				metadata -> assertSpecial( metadata, EmbeddedOwner.class ) );
	}

	@Test
	void multipleIdentifierComponents() {
		String owner = """
				<entity class="%sXmlOwner" access="FIELD" metadata-complete="true">
				<id-class class="%sOwnerKey"/>
				<attributes><id name="id"/><id name="other"/>
					<one-to-one name="extended" target-entity="%sExtendedTarget">
					<join-column name="id"/><join-column name="other"/><join-formula>1</join-formula>
					</one-to-one><transient name="target"/>
				</attributes>
				</entity>
				""".formatted( PREFIX, PREFIX, PREFIX );
		withXml( model( "8.0", owner, extendedTarget(), "" ), false, metadata -> {
			var value = (ToOne) metadata.getEntityBinding( XmlOwner.class.getName() ).getProperty( "extended" ).getValue();
			assertThat( value ).isInstanceOf( org.hibernate.mapping.OneToOne.class );
			assertThat( value.getColumnInsertability() ).containsOnly( false );
			assertThat( value.getColumnUpdateability() ).containsOnly( false );
		} );
	}

	@Test
	void sharedAndExplicitlyReadOnlyColumns() {
		withXml( model( "8.0", partialOwner( "false,false", 1, false ), extendedTarget(), "" ), false,
				metadata -> {
					var value = metadata.getEntityBinding( XmlOwner.class.getName() ).getProperty( "extended" ).getValue();
					assertThat( value ).isInstanceOf( org.hibernate.mapping.OneToOne.class );
					assertThat( value.getColumnInsertability() ).containsOnly( false );
					assertThat( value.getColumnUpdateability() ).containsOnly( false );
				} );
	}

	static Stream<Arguments> conflictingTuples() {
		return Stream.of( "true,true", "true,false", "false,true" ).flatMap( flags ->
				Stream.of( 0, 1, 2 ).flatMap( formulaPosition -> Stream.of( false, true )
						.map( reverse -> Arguments.of( flags, formulaPosition, reverse ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("conflictingTuples")
	void conflictingEffectiveFlags(String flags, int formulaPosition, boolean reverse) {
		var exception = assertThrows( MappingException.class, () ->
				withXml( model( "8.0", partialOwner( flags, formulaPosition, reverse ), extendedTarget(), "" ), false, metadata -> {} ) );
		assertThat( exception.getMessage() ).contains( "XmlOwner.extended", "id", "other", "inferred", "read-only" );
		assertThat( exception.getMessage() ).contains( flags.startsWith( "true" ) ? "insertable" : "updatable" );
	}

	@Test
	void invalidTargetIsNotMadeValidByInference() {
		String owner = simpleOwner( "", "id", "", "" )
				.replace( "<join-formula>'HOME'</join-formula>", "" );
		// Retain a formula, but provide three selectables for a two-column target key.
		owner = owner.replace( "</one-to-one>", "<join-formula>'HOME'</join-formula><join-formula>1</join-formula></one-to-one>" );
		String invalidOwner = owner;
		var exception = assertThrows( MappingException.class, () ->
				withXml( model( "8.0", invalidOwner, target(), "" ), false, metadata -> {} ) );
		assertThat( exception.getMessage() ).contains( "XmlOwner.target", "3", "2" );
	}

	@Test
	void unknownTargetReferenceIncludesAssociationPath() {
		var exception = assertThrows( MappingException.class,
				() -> withAnnotations( metadata -> {}, UnknownTargetOwner.class, AnnotatedTarget.class ) );
		assertThat( exception.getMessage() ).contains( "UnknownTargetOwner.target", "missingTargetColumn" );
	}

	@Test
	void duplicateTargetReferencesRemainInvalid() {
		var exception = assertThrows( MappingException.class,
				() -> withAnnotations( metadata -> {}, DuplicateTargetOwner.class, AnnotatedTarget.class ) );
		assertThat( exception.getMessage() ).contains( "DuplicateTargetOwner.target" );
	}

	@Test
	void annotationFlagsAndPropertyAccess() {
		withAnnotations( metadata -> {
			for ( String property : List.of( "defaults", "explicitTrue", "insertOnly", "updateOnly", "explicitFalse" ) ) {
				var value = metadata.getEntityBinding( FlagOwner.class.getName() ).getProperty( property ).getValue();
				assertThat( value ).isInstanceOf( org.hibernate.mapping.OneToOne.class );
				assertThat( value.getColumnInsertability() ).containsOnly( false );
				assertThat( value.getColumnUpdateability() ).containsOnly( false );
			}
			assertSpecial( metadata, PropertyOwner.class );
			assertSpecial( metadata, ImplicitOwner.class );
			assertSpecial( metadata, IdClassOwner.class );
			assertSpecial( metadata, AnnotatedEmbeddedOwner.class );
			assertSpecial( metadata, JoinedOwner.class );
			assertSpecial( metadata, DerivedOwner.class );
			var identityAssociation = metadata.getEntityBinding( DerivedOwner.class.getName() ).getProperty( "person" ).getValue();
			assertThat( identityAssociation ).isInstanceOf( org.hibernate.mapping.ManyToOne.class );
			assertThat( identityAssociation.getColumnInsertability() ).containsOnly( true );
		}, FlagOwner.class, PropertyOwner.class, ImplicitOwner.class, IdClassOwner.class, AnnotatedEmbeddedOwner.class,
				JoinedBase.class, JoinedOwner.class, DerivedOwner.class, Parent.class, AnnotatedTarget.class );
	}

	@Test
	void uniqueTargetReferenceRetainsInferenceAndLogicalBinding() {
		withAnnotations( metadata -> {
			var value = association( metadata, UniqueOwner.class );
			assertThat( value ).isInstanceOf( org.hibernate.mapping.ManyToOne.class );
			assertThat( value.isReferenceToPrimaryKey() ).isFalse();
			assertThat( value.getColumnInsertability() ).containsOnly( false );
			assertThat( value.getColumnUpdateability() ).containsOnly( false );
		}, UniqueOwner.class, UniqueTarget.class );
	}

	private static ToOne association(MetadataImplementor metadata, Class<?> owner) {
		return (ToOne) metadata.getEntityBinding( owner.getName() ).getProperty( "target" ).getValue();
	}

	private static void assertSpecial(MetadataImplementor metadata, Class<?> owner) {
		var value = association( metadata, owner );
		assertThat( value ).isInstanceOf( org.hibernate.mapping.OneToOne.class );
		assertThat( value.getType() ).isInstanceOf( SpecialOneToOneType.class );
		assertThat( value.getColumnInsertability() ).containsOnly( false );
		assertThat( value.getColumnUpdateability() ).containsOnly( false );
	}

	private static void withAnnotations(Consumer<MetadataImplementor> check, Class<?>... classes) {
		var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", "org.hibernate.dialect.H2Dialect" )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false ).build();
		try {
			var sources = new MetadataSources( registry );
			for ( var type : classes ) {
				sources.addAnnotatedClass( type );
			}
			var metadata = (MetadataImplementor) sources.buildMetadata();
			check.accept( metadata );
			metadata.validate();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	private static void withXml(String xml, boolean snakeCase, Consumer<MetadataImplementor> check) {
		var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", DialectContext.getDialect().getClass().getName() )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( "hibernate.hbm2ddl.auto", "create-drop" )
				.applySetting( "hibernate.validate_xml", true ).build();
		try {
			var builder = new MetadataSources( registry )
					.addInputStream( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ) ).getMetadataBuilder();
			if ( snakeCase ) {
				builder.applyPhysicalNamingStrategy( new PhysicalNamingStrategySnakeCaseImpl() );
			}
			var metadata = (MetadataImplementor) builder.build();
			check.accept( metadata );
			metadata.validate();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	private static String flags(String flags) {
		if ( flags.isEmpty() ) {
			return "";
		}
		String[] parts = flags.split( "," );
		return " insertable=\"" + parts[0] + "\" updatable=\"" + parts[1] + "\"";
	}

	private static String simpleOwner(String writeFlags, String source, String extra, String propertyRef) {
		return """
				<entity class="%sXmlOwner" access="FIELD" metadata-complete="true">
				<attributes><id name="id"><column name="%s" insertable="false" updatable="false"/></id>
				<one-to-one name="target" target-entity="%sXmlTarget">
				<join-column name="%s"%s/><join-formula>'HOME'</join-formula>%s</one-to-one>%s<transient name="extended"/><transient name="other"/>
				</attributes></entity>
				""".formatted( PREFIX, source, PREFIX, source, flags( writeFlags ), propertyRef, extra );
	}

	private static String partialOwner(String writeFlags, int formulaPosition, boolean reverse) {
		var columns = new java.util.ArrayList<>( List.of( "<join-column name=\"id\"/>", "<join-column name=\"other\"" + flags( writeFlags ) + "/>" ) );
		if ( reverse ) {
			java.util.Collections.reverse( columns );
		}
		columns.add( formulaPosition, "<join-formula>1</join-formula>" );
		return """
				<entity class="%sXmlOwner" access="FIELD" metadata-complete="true">
				<attributes><id name="id"><column insertable="false" updatable="false"/></id>
				<one-to-one name="extended" target-entity="%sExtendedTarget">%s</one-to-one><transient name="target"/><transient name="other"/>
				</attributes></entity>
				""".formatted( PREFIX, PREFIX, String.join( "", columns ) );
	}

	private static String target() {
		return "<entity class=\"" + PREFIX + "XmlTarget\" access=\"FIELD\" metadata-complete=\"true\"><attributes><embedded-id name=\"key\"/><transient name=\"lookup\"/></attributes></entity>";
	}

	private static String extendedTarget() {
		return "<entity class=\"" + PREFIX + "ExtendedTarget\" access=\"FIELD\" metadata-complete=\"true\"><id-class class=\"" + PREFIX + "ExtendedKey\"/><attributes><id name=\"id\"/><id name=\"other\"/><id name=\"tenant\"/></attributes></entity>";
	}

	private static String model(String version, String owner, String target, String extra) {
		return """
				<entity-mappings xmlns="https://www.hibernate.org/xsd/orm/mapping" version="%s">
				%s%s%s
				<embeddable class="%sKey" access="FIELD" metadata-complete="true"><attributes>
				<basic name="id"><column name="personName"/></basic><basic name="kind"><column name="addressType"/></basic>
				</attributes></embeddable>
				</entity-mappings>
				""".formatted( version, owner, target, extra, PREFIX );
	}

	public static class XmlOwner { Long id; Long other; XmlTarget target; ExtendedTarget extended; }
	public static class XmlTarget { Key key; Key lookup; }
	public static class EmbeddedOwner { Key key; XmlTarget target; }
	public static class Key implements Serializable { Long id; String kind; }
	public static class ExtendedTarget { Long id; Long other; Long tenant; }
	public record OwnerKey(Long id, Long other) implements Serializable {}
	public record ExtendedKey(Long id, Long other, Long tenant) implements Serializable {}

	@Entity
	@IdClass(TargetKey.class)
	public static class AnnotatedTarget {
		@Id @Column(name="personName") Long id;
		@Id @Column(name="addressType") String kind;
	}
	public record TargetKey(Long id, String kind) implements Serializable {}

	@Entity
	public static class UnknownTargetOwner {
		@Id Long id;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="missingTargetColumn"))
		AnnotatedTarget target;
	}
	@Entity
	public static class DuplicateTargetOwner {
		@Id Long id;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="1", referencedColumnName="personName"))
		AnnotatedTarget target;
	}

	@Entity
	public static class FlagOwner {
		@Id Long id;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget defaults;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName", insertable=true, updatable=true))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget explicitTrue;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName", insertable=true, updatable=false))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget insertOnly;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName", insertable=false, updatable=true))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget updateOnly;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName", insertable=false, updatable=false))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget explicitFalse;
	}

	@Entity
	public static class ImplicitOwner {
		@Id @Column(name="target_personName") Long id;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget target;
	}

	@Entity
	public static class PropertyOwner {
		private Long id;
		private AnnotatedTarget target;
		@Id public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		public AnnotatedTarget getTarget() { return target; }
		public void setTarget(AnnotatedTarget target) { this.target = target; }
	}

	@Entity @IdClass(OwnerKey.class)
	public static class IdClassOwner {
		@Id Long id;
		@Id Long other;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget target;
	}

	@Embeddable public static class EmbeddedOwnerKey implements Serializable { Long id; Long other; }
	@Entity
	public static class AnnotatedEmbeddedOwner {
		@EmbeddedId @AttributeOverride(name="id", column=@Column(name="ownerId")) EmbeddedOwnerKey key;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="ownerId", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget target;
	}

	@Entity @Inheritance(strategy=InheritanceType.JOINED)
	public static class JoinedBase { @Id Long id; }
	@Entity @PrimaryKeyJoinColumn(name="sub_id")
	public static class JoinedOwner extends JoinedBase {
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="sub_id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget target;
	}

	@Entity public static class Parent { @Id Long id; }
	public record DerivedKey(Long person, String kind) implements Serializable {}
	@Entity @IdClass(DerivedKey.class)
	public static class DerivedOwner {
		@Id @ManyToOne @JoinColumn(name="person_id") Parent person;
		@Id String kind;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="person_id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		AnnotatedTarget target;
	}

	@Entity public static class UniqueTarget {
		@Id Long id;
		@Column(unique=true) Long code;
		Long kind;
	}
	@Entity public static class UniqueOwner {
		@Id Long id;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="code"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="1", referencedColumnName="kind"))
		UniqueTarget target;
	}
}
