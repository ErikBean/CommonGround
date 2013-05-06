package utilities;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnglishSyllableCounter
{
	/**	Map of spellings to syllable counts. */

	protected Map syllableCountMap	=
		new HashMap();

	protected static final Pattern[] SubtractSyllables =
		new Pattern[]
		{
			Pattern.compile( "cial" ) ,
			Pattern.compile( "tia" ) ,
			Pattern.compile( "cius" ) ,
			Pattern.compile( "cious" ) ,
			Pattern.compile( "giu" ) ,	// belgium!
			Pattern.compile( "ion" ) ,
			Pattern.compile( "iou" )	,
			Pattern.compile( "sia$" ) ,
			Pattern.compile( ".ely$" )	// absolutely! (but not ely!)
		};

	protected static final Pattern[] AddSyllables =
		new Pattern[]
		{
			Pattern.compile( "ia" ),
			Pattern.compile( "riet" ),
			Pattern.compile( "dien" ),
			Pattern.compile( "iu" ),
			Pattern.compile( "io" ),
			Pattern.compile( "ii" ),
			Pattern.compile( "[aeiouym]bl$" ) ,		// -Vble, plus -mble
			Pattern.compile( "[aeiou]{3}" ) ,		// agreeable
			Pattern.compile( "^mc" ) ,
			Pattern.compile( "ism$" ) ,				// -isms
			Pattern.compile( "([^aeiouy])\1l$" ) ,	// middle twiddle battle bottle, etc.
			Pattern.compile( "[^l]lien" ) ,			// alien, salient [1]
			Pattern.compile( "^coa[dglx]." ) , 		// [2]
			Pattern.compile( "[^gq]ua[^auieo]" ) ,	// i think this fixes more than it breaks
			Pattern.compile( "dnt$" )				// couldn't
		};

	/**	Create an English syllable counter. */

	public EnglishSyllableCounter()
	{
	}

	/**	Load syllable counts map from a URL.
	 *
	 *	@param	mapURL		URL for map file.
	 *	@param 	separator	Field separator.
	 *	@param	qualifier	Quote character.
	 *	@param	encoding	Character encoding for the file.
	 *
	 *	@throws FileNotFoundException	If input file does not exist.
	 *	@throws IOException				If input file cannot be opened.
	 *
	 *	@return				Map with values read from file.
	 */



	/** Find number of syllables in a single English word.
	 *
	 *	@param	word	The word whose syllable count is desired.
	 *
	 *	@return			The number of syllables in the word.
	 */
	public int countSyllables( String word )
	{
		if(word.length()<3)return 1;//HOTFIX
		int result = 0;
								//	Null or empty word?
								//	Syllable count is zero.
		if ( ( word == null ) || ( word.length() == 0 ) )
		{
			return result;
		}
								//	If word is in the dictionary,
								//	return the syllable count from the
								//	dictionary.

		String lcWord	= word.toLowerCase();

								//	If word is not in the dictionary,
								//	use vowel group counting to get
								//	the estimated syllable count.

								//	Remove embedded apostrophes and
								//	terminal e.

		lcWord	= lcWord.replaceAll( "'" , "" ).replaceAll( "e$" , "" );

								//	Split word into vowel groups.

		String[] vowelGroups	= lcWord.split( "[^aeiouy]+" );

								//	Handle special cases.

								//	Subtract from syllable count
								//	for these patterns.

			for ( Pattern p : SubtractSyllables )
			{
				Matcher m	= p.matcher( lcWord );

				if ( m.find() )
				{
					result--;
				}
			}
								//	Add to syllable count for these patterns.

			for ( Pattern p : AddSyllables )
			{
				Matcher m	= p.matcher( lcWord );

 				if ( m.find() )
				{
					result++;
				}
			}

			if ( lcWord.length() == 1 )
			{
				result++;
			}
								//	Count vowel groupings.

			if	(	( vowelGroups.length > 0 ) &&
					( vowelGroups[ 0 ].length() == 0 )
				)
			{
				result	+= vowelGroups.length - 1;
			}
			else
			{
				result	+= vowelGroups.length;
			}
		
								//	Return syllable count of
								//	at least one.

		return Math.max( result , 1 );
	}
}