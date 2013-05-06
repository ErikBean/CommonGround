import utilities.EnglishSyllableCounter;


public class syllableTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String test="args[0]";
		EnglishSyllableCounter counter=new EnglishSyllableCounter();
		int count=counter.countSyllables(test);
		System.out.println("the string "+test+" has "+count+" syllables.");
		System.exit(0);

	}

}
