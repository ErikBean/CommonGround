package utilities;
// The Flesch Index
// Daniel Shiffman
// Programming from A to Z, Spring 2006

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import java.util.StringTokenizer;


public class FleschIndex {
	
  public double calculate(String arg) {

//    // Create an input stream and file channel
//    // Using first arguemnt as file name to read in
//    FileInputStream fis = new FileInputStream(arg);
//    FileChannel fc = fis.getChannel();
//  
//    // Read the contents of a file into a ByteBuffer
//    ByteBuffer bb = ByteBuffer.allocate((int)fc.size());
//    fc.read(bb);
//    fc.close();
    
    // Convert ByteBuffer to one long String
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

//    //Write Report
//    String report = "";
//    
//    report += "Total Syllables: " + syllables + "\n";
//    report += "Total Words    : " + words + "\n";
//    report += "Total Sentences: " + sentences + "\n";
//    report += "Flesch Index   : " + flesch + "\n";
//    System.out.println(report);
//
//    // Create an output stream and file channel to write out a report
//    // (Also print out report to screen)
//    FileOutputStream fos = new FileOutputStream("report.txt");
//    FileChannel outfc = fos.getChannel();
    
    // Convert content String into ByteBuffer and write out to file
//    bb = ByteBuffer.wrap(report.getBytes());
//    outfc.write(bb);
//    outfc.close();
    return flesch;
  }


// A method to count the number of syllables in a word
// Pretty basic, just based off of the number of vowels
// This could be improved
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