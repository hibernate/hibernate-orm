@Entity
public class Product {
	...
	@Lob
	@Basic
	public char[] description;
	...
}