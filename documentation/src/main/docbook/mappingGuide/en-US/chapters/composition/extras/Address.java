@Embeddable
public class Address {
	private String line1;
	private String line2;
	@Embedded
	private ZipCode zipCode;
	...

	@Embeddable
	public static class Zip {
		private String postalCode;
		private String plus4;
		...
	}
}