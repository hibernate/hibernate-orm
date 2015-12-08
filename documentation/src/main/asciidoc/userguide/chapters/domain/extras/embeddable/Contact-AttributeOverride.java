@Entity
public class Contact {
	@Id
	private Integer id;
	@Embedded
	private Name name;
	@Embedded
	@AttributeOverrides(
			@AttributeOverride(
					name="line1",
					column = @Column(name = "home_address_line1"),
			),
			@AttributeOverride(
					name="line2",
					column = @Column(name = "home_address_line2")
			),
			@AttributeOverride(
					name="zipCode.postalCode",
					column = @Column(name = "home_address_postal_cd")
			),
			@AttributeOverride(
					name="zipCode.plus4",
					column = @Column(name = "home_address_postal_plus4")
			)
	)
	private Address homeAddress;
	@Embedded
	@AttributeOverrides(
			@AttributeOverride(
					name="line1",
					column = @Column(name = "mailing_address_line1"),
			),
			@AttributeOverride(
					name="line2",
					column = @Column(name = "mailing_address_line2")
			),
			@AttributeOverride(
					name="zipCode.postalCode",
					column = @Column(name = "mailing_address_postal_cd")
			),
			@AttributeOverride(
					name="zipCode.plus4",
					column = @Column(name = "mailing_address_postal_plus4")
			)
	)
	private Address mailingAddress;
	@Embedded
	@AttributeOverrides(
			@AttributeOverride(
					name="line1",
					column = @Column(name = "work_address_line1"),
			),
			@AttributeOverride(
					name="line2",
					column = @Column(name = "work_address_line2")
			),
			@AttributeOverride(
					name="zipCode.postalCode",
					column = @Column(name = "work_address_postal_cd")
			),
			@AttributeOverride(
					name="zipCode.plus4",
					column = @Column(name = "work_address_postal_plus4")
			)
	)
	private Address workAddress;
	...
}