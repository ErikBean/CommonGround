import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

class CommonGround{
	final DecimalFormat DEC2 = new DecimalFormat("#.0000000");

	static HashSet<String> stopWords;
	static ConcurrentHashMap<String, Integer[]> sentDict;
	static int numCategories;
	static private String[] categoryHeaders;
	static private int[] masterCatCounts;
	static private int[] demCatCounts;
	static private int[] repCatCounts;
	static LinkedList<Word> wordCounts;
	static LinkedList<Word> demWordCounts;
	static LinkedList<Word> repWordCounts;

	@SuppressWarnings("null")
	public static void main(String[] args){
		if(args.length!=3){
			System.err.println("specify command line arguments for: [file of speech filenames]" +
					" [stop words file] [GI sentiment spreadsheet file]");
			System.exit(1);
		}
		stopWords=new HashSet<String>();
		wordCounts=new LinkedList<Word>();
		wordCounts.add(new Word(""));
		demWordCounts=new LinkedList<Word>();
		demWordCounts.add(new Word(""));
		repWordCounts=new LinkedList<Word>();
		repWordCounts.add(new Word(""));
		ParallelStopWordsLoader stopReader=new ParallelStopWordsLoader(args[1], stopWords); 
		stopReader.start();//assuming will take less time than speech, join after
		ParallelSentimentLoader sentReader=new ParallelSentimentLoader(args[2]); 
		sentReader.start();//assuming will take less time than speech, join after
		try {
			System.setOut(new PrintStream(new FileOutputStream("STAGE1.txt")));//print to file
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//fills stopwords set from file
		//In speechText = new In(args[0]);
		Scanner inFile;
		File readIn=new File(args[0]);
		HashSet<String> fnames = new HashSet<String>(20);

		try {
			inFile = new Scanner(readIn);
			while(inFile.hasNextLine()){
				
				fnames.add(inFile.nextLine().trim());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			stopReader.join();
			sentReader.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//how to keep track of dem/rep? 
		String name="";
		//using these to keep track of democrat vs republican data in output file
		int year;
		boolean democrat=false;
		LinkedList<SingleSpeechAnalyzer> threads=new LinkedList<SingleSpeechAnalyzer>();
		SingleSpeechAnalyzer sing;
		for(String s:fnames){
			name=s;
			//start some threads, baby!
			if(s.contains("txt")){
				name=s.substring(0,s.length()-4);
			}
			
			year=Integer.parseInt(s.substring(name.length()-4, name.length()));
			//System.err.println(year);
			if(year<=1952||year>=1961&&year<=1969||year>=1978&&year<=1981||year>=1993&&year<=2000||year>=2009){
				democrat=true;			}
			else{				democrat=false;			}
			sing=new SingleSpeechAnalyzer(s, sentDict, stopWords, democrat, year);
			sing.start();
			threads.add(sing);
		}
		try {
			for(SingleSpeechAnalyzer ssa:threads){
				ssa.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		compileResults();

		
		
		
	}
	private static void compileResults() {
		File resultsFile=new File("STAGE1.txt");
		String allFleschScores = "";
		String demFleschScores = "";
		String repFleschScores = "";

		StringBuilder results=new StringBuilder();
		try {
			
			Scanner inFile = new Scanner(resultsFile);
			while(inFile.hasNextLine()){
				results.append(inFile.nextLine().trim()+"\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		//this file has a LOT of data, the below are for copious splitting
		String eachResult[]=results.toString().split("NEXT");
		Arrays.sort(eachResult, new byYearComparator());
		String resultParts[];
		String resultSubParts[];
		double totalFlesch=0;
		double totalDemFlesch=0;
		double totalRepFlesch=0;
		int all=0;int dem=0;int rep=0;//counters
		for (String er:eachResult) {
			boolean democrat=false;
			resultParts=er.split("-");
			if(resultParts.length<3){continue;}
			for (int i = 1; i < resultParts.length; i++) {
				if(i==1){
					if(resultParts[i].contains("D")){		democrat=true;		}
					else{		democrat=false;				}
					//System.err.println(resultParts[0]+resultParts[1]+democrat);
				}
				if(i==2){//flesch readability scores
					allFleschScores+=resultParts[i]+",";
					totalFlesch+=Double.parseDouble(resultParts[i].trim());
					all++;
					if(democrat){			
						demFleschScores+=resultParts[i]+",";
						totalDemFlesch+=Double.parseDouble(resultParts[i].trim());
						repFleschScores+=",";
						dem++;
					}
					else{
						repFleschScores+=resultParts[i]+",";
						totalRepFlesch+=Double.parseDouble(resultParts[i].trim());
						demFleschScores+=",";
						rep++;
					}
				}
				
				//no case for WPS yet
				else if(i==4){
					resultSubParts=resultParts[i].split(",");//CATEGORIES 
					for (int k = 0; k < resultSubParts.length; k++) {
						if(k==0){resultSubParts[k]=resultSubParts[k].substring(1);}//get rid of brackets
						else if(k==resultSubParts.length-1){resultSubParts[k]=resultSubParts[k].substring(0,resultSubParts[k].length()-2);}
						resultSubParts[k]=resultSubParts[k].trim();
						masterCatCounts[k]+=Integer.parseInt(resultSubParts[k]);
						//System.err.println(j+"=>"+resultSubParts[j]);
						if(democrat){
							demCatCounts[k]+=Integer.parseInt(resultSubParts[k]);
						}
						else{
							repCatCounts[k]+=Integer.parseInt(resultSubParts[k]);
						}
					}
				}
				else if(i>4){
					String wordVal[]=resultParts[i].split(":");
					wordVal[0]=wordVal[0].trim();
					wordVal[1]=wordVal[1].trim();
					boolean alreadyFound=false;
					for(Word w:wordCounts){
						if(w.getMyWord().equals(wordVal[0])){
							w.count+=Integer.parseInt(wordVal[1]);
							alreadyFound=true;
							break;
						}
					}
					if(!alreadyFound){
						wordCounts.add(new Word(wordVal[0]));
					}
					alreadyFound=false;
					if(democrat){
						for(Word w:demWordCounts){
							if(w.getMyWord().equals(wordVal[0])){
								w.count+=Integer.parseInt(wordVal[1]);
								alreadyFound=true;
								break;
							}
						}
						if(!alreadyFound){
							demWordCounts.add(new Word(wordVal[0]));
						}
						alreadyFound=false;
					}
					else{
						for(Word w:repWordCounts){
							if(w.getMyWord().equals(wordVal[0])){
								w.count+=Integer.parseInt(wordVal[1]);
								alreadyFound=true;
								break;
							}
						}
						if(!alreadyFound){
							repWordCounts.add(new Word(wordVal[0]));
						}
						alreadyFound=false;
					}

				}//end if for wordFreq				
			}//end for each resultParts
		}//end for each result
		wordCounts.removeFirst();//remove TESTDUMMY base case
		Collections.sort(wordCounts, new ByAlphaComparator());//secondary sort: alphabetize
		Collections.sort(wordCounts, new ByWordCountComparator());//primary sort: word frequency
		demWordCounts.removeFirst();//remove TESTDUMMY base case
		Collections.sort(demWordCounts, new ByAlphaComparator());//secondary sort: alphabetize
		Collections.sort(demWordCounts, new ByWordCountComparator());//primary sort: word frequency
		repWordCounts.removeFirst();//remove TESTDUMMY base case
		Collections.sort(repWordCounts, new ByAlphaComparator());//secondary sort: alphabetize
		Collections.sort(repWordCounts, new ByWordCountComparator());//primary sort: word frequency
		//index 0=both, 1=dems, 2=reps
		int power[]=new int[3];
		int rect[]=new int[3];
		int resp[]=new int[3];
		int aff[]=new int[3];
		int wealth[]=new int[3];
		int wlb[]=new int[3];
		int enlg[]=new int[3];
		int skill[]=new int[3];
		
		for(int i=0;i<masterCatCounts.length;i++){
			//matching i to indices of value category totals
			if(i==130){				power[0]=masterCatCounts[i];			}
			else if(i==136){				rect[0]=masterCatCounts[i];			}
			else if(i==140){				resp[0]=masterCatCounts[i];			}
			else if(i==145){				aff[0]=masterCatCounts[i];			}
			else if(i==149){				wealth[0]=masterCatCounts[i];			}
			else if(i==155){				wlb[0]=masterCatCounts[i];			}
			else if(i==161){				enlg[0]=masterCatCounts[i];			}
			else if(i==165){				skill[0]=masterCatCounts[i];			}
			
			//else if(categoryHeaders[i].contains("SklTot")){System.err.println(i);}

		}
		for(int i=0;i<demCatCounts.length;i++){
			if(i==130){				power[1]=demCatCounts[i];			}
			else if(i==136){				rect[1]=demCatCounts[i];			}
			else if(i==140){				resp[1]=demCatCounts[i];			}
			else if(i==145){				aff[1]=demCatCounts[i];			}
			else if(i==149){				wealth[1]=demCatCounts[i];			}
			else if(i==155){				wlb[1]=demCatCounts[i];			}
			else if(i==161){				enlg[1]=demCatCounts[i];			}
			else if(i==165){				skill[1]=demCatCounts[i];			}
		}
		for(int i=0;i<repCatCounts.length;i++){
			if(i==130){				power[2]=repCatCounts[i];			}
			else if(i==136){				rect[2]=repCatCounts[i];			}
			else if(i==140){				resp[2]=repCatCounts[i];			}
			else if(i==145){				aff[2]=repCatCounts[i];			}
			else if(i==149){				wealth[2]=repCatCounts[i];			}
			else if(i==155){				wlb[2]=repCatCounts[i];			}
			else if(i==161){				enlg[2]=repCatCounts[i];			}
			else if(i==165){				skill[2]=repCatCounts[i];			}
		}
		try {
			System.setOut(new PrintStream(new File("STAGE2.txt")));//print to file
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		allFleschScores=allFleschScores.replaceAll("\\n\\r?", "");
		demFleschScores=demFleschScores.replaceAll("\\n\\r?", "");
		repFleschScores=repFleschScores.replaceAll("\\n\\r?", "");
		totalFlesch/=all;
		totalDemFlesch/=dem;
		totalRepFlesch/=rep;
		System.out.println("Avg Both Readability: "+totalFlesch);
		System.out.println("Avg Dem Readability: "+totalDemFlesch);
		System.out.println("Avg Rep Readability: "+totalRepFlesch);
		System.out.println(allFleschScores);
		System.out.println(demFleschScores);
		System.out.println(repFleschScores);
		for(int i=0;i<3;i++){
			if(i==0){				System.out.println("BOTH PARTIES: ");			}
			else if(i==1){	System.out.println("DEMOCRATS: ");	}
			else if(i==2){	System.out.println("REPUBLICANS:");	}
			System.out.println("Power: "+power[i]+", Rectitude: "+rect[i]+", Respect: "+resp[i]+
					", Affection: "+aff[i]+", Wealth: "+wealth[i]+", Wellbeing: "+wlb[i]+
					", Enlightenment: "+enlg[i]+", and Skill: "+skill[i]);
				
		}
		for(int i=0;i<masterCatCounts.length;i++){
			System.out.print(categoryHeaders[i]+": "+masterCatCounts[i]+", ");
		}
		System.out.println();
		for(int i=0;i<demCatCounts.length;i++){
			System.out.print(categoryHeaders[i]+": "+demCatCounts[i]+", ");
		}
		System.out.println();
		for(int i=0;i<repCatCounts.length;i++){
			System.out.print(categoryHeaders[i]+": "+repCatCounts[i]+", ");
		}
		System.out.println();
		int i=0;
		for(Word w:wordCounts){
			System.out.print(w.getMyWord()+":"+w.count+", ");
			i++;if(i>20)break;
		}
		i=0;
		System.out.println();
		for(Word w:demWordCounts){
			System.out.print(w.getMyWord()+":"+w.count+", ");
			i++;if(i>20)break;
		}
		i=0;
		System.out.println();
		for(Word w:repWordCounts){
			System.out.print(w.getMyWord()+":"+w.count+", ");
			i++;if(i>20)break;
		}
		System.out.println();
		
	
	}
	static void setSentDict(ConcurrentHashMap<String, Integer[]> loadedSentDict) {
		sentDict=loadedSentDict;
	}
	
	public static void setUpCategories(String[] catNames) {
		//ignore first 2 and last 2 dict headers
		numCategories=catNames.length-4;
		categoryHeaders=new String[numCategories];
		masterCatCounts=new int[numCategories];
		demCatCounts=new int[numCategories];
		repCatCounts=new int[numCategories];
		Arrays.fill(masterCatCounts, 0);
		for (int i = 0; i < catNames.length-4; i++) {
			categoryHeaders[i]=catNames[i+2];
		}
		
	}//end setUpCategoires
}


class SingleSpeechAnalyzer extends Thread{
	EnglishSyllableCounter counter;
	public int totalSyllables=0;
	HashSet<String> stopWords;
	LinkedList<Word> words;


	double sentenceLength;
	double numSentences;
	int numCategories;
	int[] catCounts;
	boolean democrat;
	String fname;//filename
	String[] categoryHeaders;
	ConcurrentHashMap<String, Integer[]> sentDict;//=new HashMap<String, Integer[]>(11,788);
	private int totalWords;
	private double wordsPerSentence;
	private double syllablesPerWord;
	private double fleschKincaid;
	int year;
	final DecimalFormat DEC7 = new DecimalFormat("#.0000000");
	public SingleSpeechAnalyzer(String fname, ConcurrentHashMap<String, Integer[]> sentDict, 
			HashSet<String> stopWords, boolean democrat, int year){
		this.fname=fname;
		this.sentDict=sentDict;
		this.stopWords=stopWords;
		this.catCounts=new int[CommonGround.numCategories];
		this.democrat=democrat;
		this.year=year;
	}
	
	
	public void run() {
		counter=new EnglishSyllableCounter();
		words=new LinkedList<Word>();

		//fills stopwords set from file
		//In speechText = new In(args[0]);
		Scanner inFile;
		File readIn=new File(fname);
		StringBuilder speechText=new StringBuilder();
		try {
			inFile = new Scanner(readIn);
			while(inFile.hasNextLine()){
				speechText.append(inFile.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String text = speechText.toString();
		fleschKincaid=new FleschIndex().calculate(text);		
		String singleSplit[]=preprocess(text);
		//wait for thread loading stopWords from file to finish 
		//(probably has already)



		words.add(new Word("TESTDUMMY"));
		//main loop is below
		for (int i = 0; i < singleSplit.length; i++) {
			countWordFrequency(singleSplit[i]);
			categorize(singleSplit[i]);
		}
//		for (int i = 0; i < catCounts.length; i++) {
//			System.out.print(categoryHeaders[i]+": "+catCounts[i]+", ");
//		}
		
		words.removeFirst();//remove TESTDUMMY base case
		Collections.sort(words, new ByAlphaComparator());//secondary sort: alphabetize
		Collections.sort(words, new ByWordCountComparator());//primary sort: word frequency
		print(singleSplit.length);
		return;
		//System.exit(0);
	}
	private void print(int totalWords){
		this.totalWords=totalWords;
		wordsPerSentence=(double)(totalWords/numSentences);
		String toPrint=year+"\n";
		
		if (democrat) {
			toPrint+="-D\n";
		}
		else{
			toPrint+="-R\n";
		}
		toPrint+="-"+fleschKincaid+"\n-"+wordsPerSentence+"\n-";
		toPrint+=Arrays.toString(catCounts)+"\n";
		
		for (int i = 0; i < words.size(); i++) {
			Word w=words.get(i);
			if(i<20){
				toPrint+="-"+w.getMyWord()+":"+w.count+"\n";
			}
		}
		toPrint+="NEXT\n";
		System.out.print(toPrint);
		
	}

	private String[] preprocess(String text) {
		String sentences[]=text.split("[\\.\\!\\?]");
		numSentences=sentences.length;
		String current;//current word we're looking at
		StringTokenizer stoken=new StringTokenizer(text, " .Ñ-\t\n\r\f");//added hyphens, period
		String singleSplit[]=new String[stoken.countTokens()];
		String ending;//deal with suffixes
		//replacing punctuation, removing "'s" and filling singleSplit array
		for (int i = 0; i < singleSplit.length; i++) {//housekeeping
			current=stoken.nextToken();
			if(current.length()>3){
				ending=current.substring((current.length()-2), (current.length()));
				if(ending.equals("'s")){
					current=current.substring(0, current.length()-2);
				}
			}
			current=current.replaceAll("\\p{Punct}|\\d",""); //any missed whitespace
			current=current.replaceAll("Ñ", "");//get rid of em dashes
			singleSplit[i]=current.toLowerCase();//make everything lowercase for coding
			totalSyllables+=counter.countSyllables(current);
		}
		return singleSplit;
		
	}
	public void countWordFrequency(String test){
		//std analize
		if(test.equals(" ")||test.equals(""))return;
		for(Word w:words){
			String check=w.getMyWord();
			if(test.equalsIgnoreCase(check)){
				w.increment();
				return;//found match
			}
		}
		if(!stopWords.contains(test)){
			words.add(new Word(test));
		}
		return;
	}//end countWordFrequency	
	
	private void categorize(String string) {
		Integer[] setCategories=sentDict.get(string);
		//if word is not in dict, or has no category data
		if(setCategories==null||setCategories.length==0)return;
		//System.err.println(string+Arrays.toString(setCategories));
		
		for (int i = 0; i < setCategories.length; i++) {
			catCounts[setCategories[i]]++;
		}
	}
	
	public LinkedList<Word> getWords() {		return words;	}
	public int[] getCatCounts() {	return catCounts;}

}//end main class


class Word{
	private String myWord;

	public int count;
	Word(String init){
		myWord=init;
		count=1;
	}
	public String getMyWord() {
		return myWord;
	}
	public void increment(){
		count++;
	}
}
/**This method reads in stop words from a file (the second command line argument)
 * Puts stopwords in HashSet in main class**/
class ParallelStopWordsLoader extends Thread{
	String fname;
	HashSet<String> result;
	public ParallelStopWordsLoader(String string, HashSet<String> result) {
		fname=string;
		this.result=result;
	}
	
	public void run(){
		Scanner inFile;
		File readIn=new File(fname);
		try{
			inFile=new Scanner(readIn);
			while(inFile.hasNextLine()){
				//System.out.println(inFile.nextLine());
				result.add(inFile.nextLine());
				}
		}
		catch (FileNotFoundException e){e.printStackTrace();}
		return;
	}
}


/**This method reads in the General Inquirer sentiment
 *  dictionary/spreadsheet file (the third command line argument)
 * **/
class ParallelSentimentLoader extends Thread{
	String fname;
	//public int numberOfCategories;
/**	this HashMap is represents each dictionary entry.
	Unlike the master map in the main class, this just needs to keep track of
	 true/false values for which categories it falls into.
	  The ints in the array are the category indices which the word falls into**/
	
	ConcurrentHashMap<String, Integer[]> sentDict=new ConcurrentHashMap<String, Integer[]>(11,788);//# of GI entries
	public ParallelSentimentLoader(String string) {
		fname=string;
	}
	public void run(){
		Scanner inFile = null;
		File readIn=new File(fname);
		try{
			inFile=new Scanner(readIn);
		}
		catch (FileNotFoundException e){e.printStackTrace();}
		String categoryNames=inFile.nextLine();
		int numberOfCategories=categoryNames.split(",").length-4;
		CommonGround.setUpCategories(categoryNames.split(","));
		boolean skip=false;
		while(inFile.hasNextLine()){
			String entry=inFile.nextLine();
			String[] categories=entry.split(","); 
			//first thing is the word itself
			String word=categories[0];//word string from dict
			//#after a word denotes alternate definitions (beyond scope of this project)
			boolean hash=word.contains("#");
			if(hash&&!skip){
				word=word.split("#")[0];
				//start skipping
				skip=true;//skip muliple definitions of a word
			}
			else if(hash&&skip){
				continue;
			}
			else if(!hash&&skip){
				skip=false;
				word=word.split("#")[0];
			}
			//skip next thing (dict source)
			//go through categories for word, checking if yes/no
			//System.err.println(word);
			HashSet<Integer> holder=new HashSet<Integer>();
			for (int i = 2; i < numberOfCategories-2; i++) {//ignore last 2 headers
			
				if(!categories[i].equals("")){
					//to ignore first 2 headers -word and source and last 2 - other and defn
					//the categories in main start at 0, not 2
					holder.add(i-2);
				}
			}
			//System.err.println("holder for "+word+": "+Arrays.toString(holder.toArray()));
			sentDict.put(word.toLowerCase(), holder.toArray(new Integer[0]));//put numbers into hashmap with word
		}
		//send loaded seniment dictionary back to main thread
		CommonGround.setSentDict(sentDict);
		return;
	}
}


class EnglishSyllableCounter
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
class FleschIndex {
	
	  public double calculate(String arg) {


	    String content = new String(arg);
	    
	    
	    int syllables = 0;
	    int sentences = 0;
	    int words     = 0;

	    String delimiters = ".,':;?{}[]=-+_!@#$%^&*() ";
	    StringTokenizer tokenizer = new StringTokenizer(content,delimiters);
	    //go through all words
	    while (tokenizer.hasMoreTokens())
	    {
	      String word = tokenizer.nextToken();
	      syllables += countSyllables(word);
	      words++;
	    }
	    //look for sentence delimiters
	    String sentenceDelim = ".:;?!";
	    StringTokenizer sentenceTokenizer = new StringTokenizer(content,sentenceDelim);
	    sentences = sentenceTokenizer.countTokens();
	    
	    //calculate flesch index
	    final float f1 = (float) 206.835;
	    final float f2 = (float) 84.6;
	    final float f3 = (float) 1.015;
	    float r1 = (float) syllables / (float) words;
	    float r2 = (float) words / (float) sentences;
	    float flesch = f1 - (f2*r1) - (f3*r2);

	    return flesch;
	  }

	public static int countSyllables(String word) {
	   
	    return new EnglishSyllableCounter().countSyllables(word);
	}

	//check if a char is a vowel (count y)
	public static boolean isVowel(char c) {
	    if      ((c == 'a') || (c == 'A')) { return true;  }
	    else if ((c == 'e') || (c == 'E')) { return true;  }
	    else if ((c == 'i') || (c == 'I')) { return true;  }
	    else if ((c == 'o') || (c == 'O')) { return true;  }
	    else if ((c == 'u') || (c == 'U')) { return true;  }
	    else if ((c == 'y') || (c == 'Y')) { return true;  }
	    else                               { return false; }
	  }
	}

/**sorts list of counted words by frequency**/
class ByWordCountComparator implements Comparator<Word>{
	@Override
	public int compare(Word a, Word b) {
		return b.count-a.count;
	}
}
/**sorts list of counted words by alphabetical order**/
class ByAlphaComparator implements Comparator<Word>{

	@Override
	public int compare(Word a, Word b) {

		return a.getMyWord().compareTo(b.getMyWord());
	}
}
class byYearComparator implements Comparator<String>{
	String name="";int yearA=0;int yearB=0;

	@Override
	public int compare(String x, String y) {
		if(x.length()<3)return -1;
		else if(y.length()<3)return 1;
		yearA=Integer.parseInt(x.split("-")[0].trim());
		yearB=Integer.parseInt(y.split("-")[0].trim());
		//System.err.println(yearA+" & "+yearB);
		return yearA-yearB;
	}
	
}